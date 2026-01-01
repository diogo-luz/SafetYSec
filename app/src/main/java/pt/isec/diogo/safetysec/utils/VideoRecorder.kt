package pt.isec.diogo.safetysec.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

/**
 * tratamos diretamente da gravacao e do upload para o firebasestorage
 */
class VideoRecorder(private val context: Context) {
    
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    companion object {
        private const val TAG = "VideoRecorder"
        private const val RECORDING_DURATION_MS = 30_000L // 30 segundos
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    
    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
            
            // Video capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // frontal para SOS
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
                onReady()
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    @androidx.annotation.OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
    fun startRecording(
        onProgress: (Int) -> Unit,
        onComplete: (Uri?) -> Unit,
        onError: (String) -> Unit
    ) {
        val videoCapture = this.videoCapture ?: run {
            onError("Camera not initialized")
            return
        }
        
        // Nome do ficheiro
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "SOS_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SafetYSec")
            }
        }
        
        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        
        var elapsedSeconds = 0
        
        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Log.d(TAG, "Recording started")
                        // timer de 30s
                        startRecordingTimer(onProgress) {
                            stopRecording()
                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val uri = recordEvent.outputResults.outputUri
                            Log.d(TAG, "Recording saved: $uri")
                            onComplete(uri)
                        } else {
                            Log.e(TAG, "Recording error: ${recordEvent.error}")
                            onError("Recording failed: ${recordEvent.error}")
                        }
                        recording = null
                    }
                }
            }
    }
    
    private fun startRecordingTimer(
        onProgress: (Int) -> Unit,
        onTimeout: () -> Unit
    ) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var secondsRemaining = 30
        
        val runnable = object : Runnable {
            override fun run() {
                if (secondsRemaining > 0) {
                    onProgress(secondsRemaining)
                    secondsRemaining--
                    handler.postDelayed(this, 1000)
                } else {
                    onTimeout()
                }
            }
        }
        handler.post(runnable)
    }
    
    fun stopRecording() {
        recording?.stop()
        recording = null
    }
    
    fun release() {
        stopRecording()
        cameraProvider?.unbindAll()
    }
    
    suspend fun uploadToFirebaseStorage(
        localUri: Uri,
        alertId: String
    ): String? = suspendCancellableCoroutine { continuation ->
        val storageRef = FirebaseStorage.getInstance().reference
        val videoRef = storageRef.child("alert_videos/$alertId.mp4")
        
        val uploadTask = videoRef.putFile(localUri)
        
        uploadTask
            .addOnSuccessListener {
                videoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d(TAG, "Upload success: $downloadUri")
                    continuation.resume(downloadUri.toString())
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get download URL", e)
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Upload failed", e)
                continuation.resume(null)
            }
        
        continuation.invokeOnCancellation {
            uploadTask.cancel()
        }
    }
}
