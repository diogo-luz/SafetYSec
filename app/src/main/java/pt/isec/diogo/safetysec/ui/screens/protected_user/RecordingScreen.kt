package pt.isec.diogo.safetysec.ui.screens.protected_user

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.utils.VideoRecorder

enum class RecordingState {
    REQUESTING_PERMISSION,
    INITIALIZING,
    RECORDING,
    UPLOADING,
    COMPLETE,
    ERROR
}

@Composable
fun RecordingScreen(
    alertId: String,
    onRecordingComplete: (String?) -> Unit, // videoUrl/null
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var recordingState by remember { mutableStateOf(RecordingState.REQUESTING_PERMISSION) }
    var secondsRemaining by remember { mutableIntStateOf(30) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localVideoUri by remember { mutableStateOf<Uri?>(null) }
    
    val videoRecorder = remember { VideoRecorder(context) }
    
    // Permissões
    val hasPermissions = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        
        if (cameraGranted && audioGranted) {
            recordingState = RecordingState.INITIALIZING
        } else {
            recordingState = RecordingState.ERROR
            errorMessage = "Camera and microphone permissions required"
            onError("Permissions denied")
        }
    }
    
    // Pedir permissões
    LaunchedEffect(Unit) {
        if (hasPermissions) {
            recordingState = RecordingState.INITIALIZING
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }
    
    // release dos recursos da cam
    DisposableEffect(Unit) {
        onDispose {
            videoRecorder.release()
        }
    }
    
    //upload do video
    LaunchedEffect(localVideoUri) {
        localVideoUri?.let { uri ->
            recordingState = RecordingState.UPLOADING
            scope.launch {
                val videoUrl = videoRecorder.uploadToFirebaseStorage(uri, alertId)
                if (videoUrl != null) {
                    recordingState = RecordingState.COMPLETE
                    onRecordingComplete(videoUrl)
                } else {
                    recordingState = RecordingState.ERROR
                    errorMessage = "Failed to upload video"
                    onRecordingComplete(null) // Continuar mesmo sem vídeo
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (recordingState) {
            RecordingState.REQUESTING_PERMISSION -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.requesting_permissions),
                        color = Color.White
                    )
                }
            }
            
            RecordingState.INITIALIZING,
            RecordingState.RECORDING -> {
                var previewView by remember { mutableStateOf<PreviewView?>(null) }
                
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // init da cam e gravacao
                LaunchedEffect(previewView) {
                    previewView?.let { view ->
                        videoRecorder.setupCamera(lifecycleOwner, view) {
                            recordingState = RecordingState.RECORDING
                            videoRecorder.startRecording(
                                onProgress = { remaining ->
                                    secondsRemaining = remaining
                                },
                                onComplete = { uri ->
                                    localVideoUri = uri
                                },
                                onError = { error ->
                                    recordingState = RecordingState.ERROR
                                    errorMessage = error
                                    onError(error)
                                }
                            )
                        }
                    }
                }
                
                // overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = stringResource(R.string.recording),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "${secondsRemaining}s",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            RecordingState.UPLOADING -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.uploading_video),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
            
            RecordingState.COMPLETE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.Green,
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.video_uploaded),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
            
            RecordingState.ERROR -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠",
                        color = Color.Red,
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Recording failed",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
