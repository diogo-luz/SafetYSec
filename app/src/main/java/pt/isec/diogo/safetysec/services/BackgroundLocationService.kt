package pt.isec.diogo.safetysec.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.MainActivity
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Alert
import pt.isec.diogo.safetysec.data.model.AlertStatus
import pt.isec.diogo.safetysec.data.model.AlertTriggerType
import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleType
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.AlertsRepository
import pt.isec.diogo.safetysec.data.repository.AuthRepository
import pt.isec.diogo.safetysec.data.repository.RulesRepository
import pt.isec.diogo.safetysec.utils.GeofenceChecker
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.sqrt

class BackgroundLocationService : Service(), SensorEventListener {
    
    companion object {
        private const val TAG = "BackgroundLocationSvc"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "location_service_channel"
        private const val ALERT_CHANNEL_ID = "alert_channel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CANCEL_COUNTDOWN = "ACTION_CANCEL_COUNTDOWN"
        const val EXTRA_TRIGGER_TYPE = "trigger_type"
        const val EXTRA_SECONDS_LEFT = "seconds_left"
        
        // Estado partilhado para sincronizar com UI
        var isServiceRunning = false
            private set
        var isCountdownActive = false
            private set
        var currentSecondsLeft = 0
            private set
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var rulesRepository: RulesRepository
    private lateinit var alertsRepository: AlertsRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var geofenceChecker: GeofenceChecker
    
    // Sensor Manager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private var lastLocation: Location? = null
    private var geofenceRules: List<Rule> = emptyList()
    private var speedLimitKmh: Double? = null
    
    // Flags de regras de sensores (para bateria!)
    private var isFallDetectionActive = false
    private var isAccidentDetectionActive = false
    
    private var currentUserId: String? = null
    private var currentUser: User? = null
    
    private var isAlertTriggered = false
    private var countdownTimer: CountDownTimer? = null
    private var pendingTriggerType: AlertTriggerType? = null
    
    // Cooldown sensores
    private var lastSensorTriggerTime: Long = 0
    private val SENSOR_COOLDOWN_MS = 5000L // 5 segundos entre deteções
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                onLocationUpdate(location)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        rulesRepository = RulesRepository()
        alertsRepository = AlertsRepository()
        authRepository = AuthRepository()
        geofenceChecker = GeofenceChecker()
        
        // init sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLocationMonitoring()
            ACTION_STOP -> stopSelf()
            ACTION_CANCEL_COUNTDOWN -> cancelCountdown()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopSensorMonitoring() // Garante que para sensores
        countdownTimer?.cancel()
        isCountdownActive = false
        isServiceRunning = false
        serviceScope.cancel()
    }
    
    private fun startLocationMonitoring() {
        Log.d(TAG, "Starting location monitoring")
        isServiceRunning = true
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        loadUserAndRules()
        startLocationUpdates()
    }
    
    private fun startSensorMonitoring() {
        if (accelerometer == null) return
        
        try {
            sensorManager.unregisterListener(this) // Limpar primeiro
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Sensor monitoring started (Fall: $isFallDetectionActive, Accident: $isAccidentDetectionActive)")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting sensor monitoring", e)
        }
    }
    
    private fun stopSensorMonitoring() {
        try {
            sensorManager.unregisterListener(this)
            Log.d(TAG, "Sensor monitoring stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sensor monitoring", e)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                if (isAlertTriggered || isCountdownActive) return
                if (System.currentTimeMillis() - lastSensorTriggerTime < SENSOR_COOLDOWN_MS) return
                
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                
                // força-g -> aula teórica
                val magnitude = sqrt(x * x + y * y + z * z)
                val gForce = magnitude / 9.81
                
                // Thresholds
                val FALL_THRESHOLD_G = 2.5 // Queda (~2.5G)
                val ACCIDENT_THRESHOLD_G = 4.0 // Acidente (~4.0G)
                
                when {
                    isAccidentDetectionActive && gForce > ACCIDENT_THRESHOLD_G -> {
                        Log.w(TAG, "Accident Detected! G-Force: $gForce")
                        lastSensorTriggerTime = System.currentTimeMillis()
                        triggerAlert(AlertTriggerType.ACCIDENT_DETECTION)
                    }
                    isFallDetectionActive && gForce > FALL_THRESHOLD_G -> {
                        Log.w(TAG, "Fall Detected! G-Force: $gForce")
                        lastSensorTriggerTime = System.currentTimeMillis()
                        triggerAlert(AlertTriggerType.FALL_DETECTION)
                    }
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // yolo
    }
    
    private fun loadUserAndRules() {
        val userId = currentUserId ?: return
        
        // Reset flags
        isFallDetectionActive = false
        isAccidentDetectionActive = false
        geofenceRules = emptyList()
        speedLimitKmh = null
        
        serviceScope.launch {
            // Carregar dados (nome e cancellationTimer)
            authRepository.getCurrentUser().onSuccess { user ->
                currentUser = user
                Log.d(TAG, "Loaded user: ${user.displayName}, timer: ${user.cancellationTimer}s")
            }.onFailure {
                Log.e(TAG, "Failed to load user", it)
            }
            
            // Carregar regras do user
            rulesRepository.getAssignmentsForProtected(userId).onSuccess { rulesWithAssignments ->
                rulesWithAssignments.forEach { (rule, assignment) ->
                    if (assignment.isAccepted && rule.isActive) {
                        when (rule.type) {
                            RuleType.SPEED_LIMIT -> {
                                speedLimitKmh = rule.threshold
                                Log.d(TAG, "Speed limit set to $speedLimitKmh km/h")
                            }
                            RuleType.GEOFENCE -> {
                                geofenceRules = geofenceRules + rule
                                Log.d(TAG, "Added geofence: ${rule.name}")
                            }
                            RuleType.FALL_DETECTION -> {
                                isFallDetectionActive = true
                                Log.d(TAG, "Fall Detection Active")
                            }
                            RuleType.ACCIDENT_DETECTION -> {
                                isAccidentDetectionActive = true
                                Log.d(TAG, "Accident Detection Active")
                            }
                            else -> {}
                        }
                    }
                }
                Log.d(TAG, "Loaded Rules: Geo(${geofenceRules.size}), Speed($speedLimitKmh), Fall($isFallDetectionActive), Accident($isAccidentDetectionActive)")
                
                // Ativar/Desativar sensores com base nas regras
                if (isFallDetectionActive || isAccidentDetectionActive) {
                    startSensorMonitoring()
                } else {
                    stopSensorMonitoring()
                }
            }
        }
    }
    
    @android.annotation.SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()
        
        locationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }
    
    private fun onLocationUpdate(location: Location) {
        // Não verificar regras se já há alerta ativo ou countdown
        if (isAlertTriggered || isCountdownActive) return
        
        checkGeofence(location)
        checkSpeed(location)
        
        lastLocation = location
    }
    
    private fun checkGeofence(location: Location) {
        if (geofenceRules.isEmpty()) return
        
        val isInsideAnyZone = geofenceChecker.isInsideAnyZone(
            latitude = location.latitude,
            longitude = location.longitude,
            geofenceRules = geofenceRules
        )
        
        if (!isInsideAnyZone) {
            Log.w(TAG, "User outside all safe zones!")
            triggerAlert(AlertTriggerType.GEOFENCE_VIOLATION)
        }
    }
    
    private fun checkSpeed(location: Location) {
        val limit = speedLimitKmh ?: return
        val speedKmh = location.speed * 3.6
        
        if (speedKmh > limit) {
            Log.w(TAG, "Speed limit exceeded: $speedKmh km/h (limit: $limit)")
            triggerAlert(AlertTriggerType.SPEED_LIMIT)
        }
    }
    
    private fun triggerAlert(triggerType: AlertTriggerType) {
        if (isAlertTriggered || isCountdownActive) return
        
        isCountdownActive = true
        pendingTriggerType = triggerType
        
        // Usar cancellationTimer do utilizador (default 10s)
        val timerSeconds = currentUser?.cancellationTimer ?: 10
        val timerMs = timerSeconds * 1000L
        
        Log.d(TAG, "Starting countdown: ${timerSeconds}s for ${triggerType.name}")
        
        showCountdownNotification(triggerType, timerSeconds)
        
        countdownTimer = object : CountDownTimer(timerMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentSecondsLeft = (millisUntilFinished / 1000).toInt()
                showCountdownNotification(triggerType, currentSecondsLeft)
            }
            
            override fun onFinish() {
                Log.d(TAG, "Countdown finished - creating alert")
                isCountdownActive = false
                currentSecondsLeft = 0
                createAlertAndNotify(triggerType)
            }
        }.start()
    }
    
    fun cancelCountdown() {
        Log.d(TAG, "Countdown cancelled")
        countdownTimer?.cancel()
        countdownTimer = null
        pendingTriggerType = null
        isCountdownActive = false
        currentSecondsLeft = 0
        
        // Remover notificação de alerta
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
        
        // Cooldown antes de poder disparar outro alerta
        isAlertTriggered = true
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            isAlertTriggered = false
            Log.d(TAG, "Cooldown ended - ready for new alerts")
        }, 30000) // 30 segundos de cooldown
    }
    
    private fun createAlertAndNotify(triggerType: AlertTriggerType) {
        val userId = currentUserId ?: return
        val userName = currentUser?.displayName ?: ""
        
        isAlertTriggered = true
        
        serviceScope.launch {
            val alert = Alert(
                protectedUserId = userId,
                protectedUserName = userName,
                triggerType = triggerType,
                status = AlertStatus.ACTIVE,
                latitude = lastLocation?.latitude,
                longitude = lastLocation?.longitude
            )
            
            alertsRepository.createAlert(alert).onSuccess { alertId ->
                Log.d(TAG, "Alert created with ID: $alertId")
                showAlertCreatedNotification(triggerType, alertId)
            }.onFailure { e ->
                Log.e(TAG, "Failed to create alert", e)
            }
            
            // Cooldown antes de poder disparar outro alerta
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                isAlertTriggered = false
                Log.d(TAG, "Cooldown ended - ready for new alerts")
            }, 60000) // 60 segundos de cooldown
        }
    }
    
    private fun showCountdownNotification(triggerType: AlertTriggerType, secondsLeft: Int) {
        val alertTitle = when (triggerType) {
            AlertTriggerType.GEOFENCE_VIOLATION -> "Left Safe Zone!"
            AlertTriggerType.SPEED_LIMIT -> "Speed Limit Exceeded!"
            AlertTriggerType.FALL_DETECTION -> "Fall Detected!"
            AlertTriggerType.ACCIDENT_DETECTION -> "Accident Detected!"
            else -> "Safety Alert!"
        }
        
        val cancelIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TRIGGER_TYPE, triggerType.name)
            putExtra(EXTRA_SECONDS_LEFT, secondsLeft)
            action = "TRIGGER_SOS_COUNTDOWN"
        }
        
        val cancelPendingIntent = PendingIntent.getActivity(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val totalSeconds = currentUser?.cancellationTimer ?: 10
        
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(alertTitle)
            .setContentText("Alert in $secondsLeft seconds - Tap to cancel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(cancelPendingIntent)
            .setFullScreenIntent(cancelPendingIntent, true)
            .setOngoing(true)
            .setProgress(totalSeconds, secondsLeft, false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
    
    private fun showAlertCreatedNotification(triggerType: AlertTriggerType, alertId: String) {
        val alertTitle = when (triggerType) {
            AlertTriggerType.GEOFENCE_VIOLATION -> "Alert Sent - Left Safe Zone"
            AlertTriggerType.SPEED_LIMIT -> "Alert Sent - Speed Limit"
            AlertTriggerType.FALL_DETECTION -> "Alert Sent - Fall Detected"
            AlertTriggerType.ACCIDENT_DETECTION -> "Alert Sent - Accident Detected"
            else -> "Alert Sent"
        }
        
        val recordIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALERT_ID", alertId)
            action = "OPEN_RECORDING"
        }
        
        val recordPendingIntent = PendingIntent.getActivity(
            this, 1, recordIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(alertTitle)
            .setContentText("Monitors notified - Tap to record video")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(recordPendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SafetYSec is monitoring your location"
            }
            
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical safety alerts"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoring your safety")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
