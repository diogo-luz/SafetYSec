package pt.isec.diogo.safetysec.ui.screens.protected_user

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.AlertTriggerType
import pt.isec.diogo.safetysec.data.model.User

@Composable
fun SOSCountdownScreen(
    currentUser: User?,
    triggerType: AlertTriggerType = AlertTriggerType.MANUAL_SOS,
    onAlertTriggered: () -> Unit,
    onCancelled: () -> Unit
) {
    val totalSeconds = currentUser?.cancellationTimer ?: 10
    val userPin = currentUser?.cancellationPin ?: "0000"
    
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    
    // Mensagens e ícone baseados no trigger type
    val (title, subtitle, icon) = getTriggerTypeContent(triggerType)

    // Countdown timer
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        } else {
            // trigger do alerta no fim do countdown
            onAlertTriggered()
        }
    }

    val progress by animateFloatAsState(
        targetValue = remainingSeconds.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 500),
        label = "countdown_progress"
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icone
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Countdown
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = remainingSeconds.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Cancel
            OutlinedButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(R.string.cancel_sos),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // PIN Cancelar
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCancelDialog = false
                pinInput = ""
                pinError = false
            },
            title = { 
                Text(stringResource(R.string.enter_pin_to_cancel)) 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                pinInput = newValue
                                pinError = false
                            }
                        },
                        label = { Text(stringResource(R.string.pin)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError
                    )
                    if (pinError) {
                        Text(
                            text = stringResource(R.string.incorrect_pin),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == userPin) {
                            showCancelDialog = false
                            onCancelled()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCancelDialog = false
                        pinInput = ""
                        pinError = false
                    }
                ) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }
}

@Composable
private fun CircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val errorColor = MaterialTheme.colorScheme.error
    val backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val strokeWidth = 16.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            (size.width - diameter) / 2,
            (size.height - diameter) / 2
        )

        // Background circulo
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // anel progresso
        drawArc(
            color = errorColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun getTriggerTypeContent(triggerType: AlertTriggerType): Triple<String, String, ImageVector> {
    return when (triggerType) {
        AlertTriggerType.MANUAL_SOS -> Triple(
            stringResource(R.string.sos_countdown_title),
            stringResource(R.string.sos_countdown_subtitle),
            Icons.Default.Warning
        )
        AlertTriggerType.GEOFENCE_VIOLATION -> Triple(
            stringResource(R.string.geofence_alert_title),
            stringResource(R.string.geofence_alert_subtitle),
            Icons.Default.LocationOff
        )
        AlertTriggerType.SPEED_LIMIT -> Triple(
            stringResource(R.string.speed_alert_title),
            stringResource(R.string.speed_alert_subtitle),
            Icons.Default.DirectionsCar
        )
        AlertTriggerType.FALL_DETECTION -> Triple(
            stringResource(R.string.fall_alert_title),
            stringResource(R.string.fall_alert_subtitle),
            Icons.Default.Warning
        )
        AlertTriggerType.ACCIDENT_DETECTION -> Triple(
            stringResource(R.string.accident_alert_title),
            stringResource(R.string.accident_alert_subtitle),
            Icons.Default.Warning
        )
        AlertTriggerType.INACTIVITY -> Triple(
            stringResource(R.string.inactivity_alert_title),
            stringResource(R.string.inactivity_alert_subtitle),
            Icons.Default.Warning
        )
    }
}
