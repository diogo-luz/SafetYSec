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
import android.os.Handler
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
import pt.isec.diogo.safetysec.data.model.RuleAssignment
import pt.isec.diogo.safetysec.data.model.RuleType
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.AlertsRepository
import pt.isec.diogo.safetysec.data.repository.AuthRepository
import pt.isec.diogo.safetysec.data.repository.RulesRepository
import pt.isec.diogo.safetysec.utils.GeofenceChecker
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs
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
        const val ACTION_RELOAD_RULES = "ACTION_RELOAD_RULES"
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
    
    // Delta G-Force (inicializar a 1.0 = gravidade padrão para evitar falso positivo na 1ª leitura)
    private var lastGForce: Double = 1.0
    
    private var lastLocation: Location? = null
    private var geofenceRules: List<Rule> = emptyList()
    private var speedLimitKmh: Double? = null
    
    // Flags de regras de sensores (para bateria!)
    private var isFallDetectionActive = false
    private var isAccidentDetectionActive = false
    
    // Inatividade
    private var isInactivityMonitorActive = false
    private var inactivityThresholdMinutes: Double? = null
    private var lastMovementTime: Long = 0
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private var scheduleCheckCounter = 0
    private val inactivityRunnable = object : Runnable {
        override fun run() {
            checkInactivity()
            
            // A cada 5 minutos, recarregar regras para verificar horários
            scheduleCheckCounter++
            if (scheduleCheckCounter >= 5) {
                scheduleCheckCounter = 0
                Log.d(TAG, "Periodic schedule check - reloading rules")
                loadUserAndRules()
            }
            
            inactivityHandler.postDelayed(this, 60000) // Verificar a cada 1 minuto
        }
    }
    
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
        
        // iniciar timestamp de movimento
        lastMovementTime = System.currentTimeMillis()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startLocationMonitoring()
            ACTION_STOP -> stopSelf()
            ACTION_CANCEL_COUNTDOWN -> cancelCountdown()
            ACTION_RELOAD_RULES -> {
                Log.d(TAG, "Reloading rules...")
                loadUserAndRules()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopSensorMonitoring() 
        inactivityHandler.removeCallbacks(inactivityRunnable)
        countdownTimer?.cancel()
        isCountdownActive = false
        isServiceRunning = false
        serviceScope.cancel()
    }
    
    private fun startLocationMonitoring() {
        Log.d(TAG, "Starting location monitoring")
        isServiceRunning = true
        lastMovementTime = System.currentTimeMillis() // Reset movement timer quando começa
        
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        loadUserAndRules()
        startLocationUpdates()
        // Inicia loop de inatividade
        inactivityHandler.post(inactivityRunnable)
    }
    
    private fun startSensorMonitoring() {
        if (accelerometer == null) return
        
        try {
            sensorManager.unregisterListener(this) 
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Sensor monitoring started (Fall: $isFallDetectionActive, Accident: $isAccidentDetectionActive, Inactivity: $isInactivityMonitorActive)")
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
                
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                
                // Força-G Atual
                val magnitude = sqrt(x * x + y * y + z * z)
                val gForce = magnitude / 9.81
                
                // Deteção de Movimento para Inatividade (Delta G-Force)
                if (isInactivityMonitorActive) {
                    val delta = abs(gForce - lastGForce)
                    if (delta > 0.5) { // Movimento significativo
                         lastMovementTime = System.currentTimeMillis()
                         Log.v(TAG, "Motion detected due to delta G: $delta")
                    }
                    lastGForce = gForce
                }
                
                // Cooldown para alertas de sensores (Queda/Acidente)
                if (System.currentTimeMillis() - lastSensorTriggerTime < SENSOR_COOLDOWN_MS) return
                
                // Thresholds
                val FALL_THRESHOLD_G = 2.5 // Queda
                val ACCIDENT_THRESHOLD_G = 4.0 // Acidente
                
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
        isInactivityMonitorActive = false
        inactivityThresholdMinutes = null
        
        geofenceRules = emptyList()
        speedLimitKmh = null
        
        serviceScope.launch {
            authRepository.getCurrentUser().onSuccess { user ->
                currentUser = user
            }
            
            rulesRepository.getAssignmentsForProtected(userId).onSuccess { rulesWithAssignments ->
                rulesWithAssignments.forEach { (rule, assignment) ->
                    // Verificar se aceite, ativa E dentro do horário
                    if (assignment.isAccepted && rule.isActive && isWithinSchedule(assignment)) {
                        when (rule.type) {
                            RuleType.SPEED_LIMIT -> {
                                speedLimitKmh = rule.threshold
                                Log.d(TAG, "Speed limit: $speedLimitKmh km/h")
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
                            RuleType.INACTIVITY -> {
                                isInactivityMonitorActive = true
                                inactivityThresholdMinutes = rule.threshold
                                Log.d(TAG, "Inactivity Monitor Active: $inactivityThresholdMinutes min")
                            }
                            else -> {}
                        }
                    }
                }
                
                Log.d(TAG, "Rules loaded: Geo(${geofenceRules.size}), Speed($speedLimitKmh), Fall($isFallDetectionActive), Acc($isAccidentDetectionActive), Inact($isInactivityMonitorActive)")
                
                // Ativar sensores se qualquer regra que precise deles estiver ativa
                if (isFallDetectionActive || isAccidentDetectionActive || isInactivityMonitorActive) {
                    startSensorMonitoring()
                } else {
                    stopSensorMonitoring()
                }
            }
        }
    }
    
    /**
     * Verifica se a hora atual está dentro do horário agendado da regra.
     * Se não houver horário definido (startTime/endTime null), a regra está sempre ativa.
     */
    private fun isWithinSchedule(assignment: RuleAssignment): Boolean {
        val startTime = assignment.startTime
        val endTime = assignment.endTime
        
        // Sem horário = sempre ativo
        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty()) {
            return true
        }
        
        try {
            val now = java.util.Calendar.getInstance()
            val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
            
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()
            
            // Verificação simples (não atravessa meia-noite)
            val isWithin = currentMinutes in startMinutes..endMinutes
            
            if (!isWithin) {
                Log.d(TAG, "Rule outside schedule: current=$currentMinutes, range=$startMinutes-$endMinutes")
            }
            
            return isWithin
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing schedule", e)
            return true // Em caso de erro, permitir regra
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
        if (isAlertTriggered || isCountdownActive) return
        
        // Deteta movimento pelo GPS (speed está em m/s, 1.5 m/s ≈ 5.4 km/h = caminhada)
        if (isInactivityMonitorActive) {
            val speedKmh = location.speed * 3.6
            if (speedKmh > 5.0) { // > 5 km/h para evitar GPS drift
                Log.d(TAG, "GPS motion detected: $speedKmh km/h")
                lastMovementTime = System.currentTimeMillis()
            }
        }
        
        checkGeofence(location)
        checkSpeed(location)
        
        lastLocation = location
    }
    
    private fun checkInactivity() {
        if (!isServiceRunning) {
            Log.d(TAG, "checkInactivity: service not running")
            return
        }
        if (!isInactivityMonitorActive) {
            Log.d(TAG, "checkInactivity: inactivity monitor not active")
            return
        }
        if (isAlertTriggered || isCountdownActive) {
            Log.d(TAG, "checkInactivity: alert in progress")
            return
        }
        
        val thresholdMin = inactivityThresholdMinutes ?: return
        val thresholdMs = (thresholdMin * 60 * 1000).toLong()
        val timeSinceLastMove = System.currentTimeMillis() - lastMovementTime
        
        Log.d(TAG, "checkInactivity: timeSinceLastMove=${timeSinceLastMove/1000}s, threshold=${thresholdMs/1000}s")
        
        if (timeSinceLastMove >= thresholdMs) {
            Log.w(TAG, "Inactivity Detected! No movement for ${timeSinceLastMove/1000}s")
            lastMovementTime = System.currentTimeMillis() 
            triggerAlert(AlertTriggerType.INACTIVITY)
        }
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
            Log.w(TAG, "Speed limit exceeded: $speedKmh km/h")
            triggerAlert(AlertTriggerType.SPEED_LIMIT)
        }
    }
    
    private fun triggerAlert(triggerType: AlertTriggerType) {
        if (isAlertTriggered || isCountdownActive) return
        
        isCountdownActive = true
        pendingTriggerType = triggerType
        
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
        lastMovementTime = System.currentTimeMillis() // Reset inactivity on interaction
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
        
        isAlertTriggered = true
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            isAlertTriggered = false
            Log.d(TAG, "Cooldown ended")
        }, 30000)
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
            
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                isAlertTriggered = false
                Log.d(TAG, "Cooldown ended")
            }, 60000)
        }
    }
    
    private fun showCountdownNotification(triggerType: AlertTriggerType, secondsLeft: Int) {
        val alertTitle = when (triggerType) {
            AlertTriggerType.GEOFENCE_VIOLATION -> getString(R.string.alert_title_geofence)
            AlertTriggerType.SPEED_LIMIT -> getString(R.string.alert_title_speed)
            AlertTriggerType.FALL_DETECTION -> getString(R.string.alert_title_fall)
            AlertTriggerType.ACCIDENT_DETECTION -> getString(R.string.alert_title_accident)
            AlertTriggerType.INACTIVITY -> getString(R.string.alert_title_inactivity)
            else -> getString(R.string.alert_title_safety)
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
            .setContentText(getString(R.string.alert_countdown_message, secondsLeft))
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
            AlertTriggerType.GEOFENCE_VIOLATION -> getString(R.string.alert_sent_geofence)
            AlertTriggerType.SPEED_LIMIT -> getString(R.string.alert_sent_speed)
            AlertTriggerType.FALL_DETECTION -> getString(R.string.alert_sent_fall)
            AlertTriggerType.ACCIDENT_DETECTION -> getString(R.string.alert_sent_accident)
            AlertTriggerType.INACTIVITY -> getString(R.string.alert_sent_inactivity)
            else -> getString(R.string.alert_sent_generic)
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
            .setContentText(getString(R.string.alert_monitors_notified))
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
                getString(R.string.notification_channel_location),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_location_desc)
            }
            
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_alerts_desc)
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
            .setContentText(getString(R.string.notification_monitoring_safety))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
