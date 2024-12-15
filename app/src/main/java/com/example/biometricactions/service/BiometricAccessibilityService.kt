package com.example.biometricactions.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.hardware.fingerprint.FingerprintManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.biometricactions.util.AuthSessionManager
import com.example.biometricactions.util.PreferencesManager
import kotlin.math.abs

@Suppress("DEPRECATION")
class BiometricAccessibilityService : AccessibilityService(), SensorEventListener {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var fingerprintManager: FingerprintManager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var cancellationSignal: CancellationSignal? = null
    private var isScanning = false
    private var lastTapTime = 0L
    private var tapCount = 0
    private val longPressThreshold = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private var isProcessingGesture = false
    private lateinit var authSessionManager: AuthSessionManager

    // Shake detection variables
    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastUpdate = 0L
    private var isShakeGestureInProgress = false

    private var isFingerDown = false

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen turned ON")
                    handler.postDelayed({
                        resetGestureState()
                        startFingerprintScanning()
                    }, 1000)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen turned OFF")
                    stopFingerprintScanning()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Device unlocked")
                    handler.postDelayed({
                        resetGestureState()
                        startFingerprintScanning()
                    }, 1000)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        authSessionManager = AuthSessionManager(this)
        fingerprintManager = getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        
        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Register screen state receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
        
        // Create and start foreground notification
        startForeground(NOTIFICATION_ID, createForegroundNotification())
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
            notificationTimeout = 100
        }
        serviceInfo = info
        
        startFingerprintScanning()
        startShakeDetection()
    }

    private fun startFingerprintScanning() {
        if (isScanning) {
            stopFingerprintScanning()
        }

        cancellationSignal = CancellationSignal()
        isScanning = true
        
        try {
            Log.d(TAG, "Starting fingerprint scanning")
            fingerprintManager.authenticate(
                null,
                cancellationSignal,
                0,
                object : FingerprintManager.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        
                        if (isProcessingGesture) {
                            return
                        }

                        val currentTime = System.currentTimeMillis()
                        lastTapTime = currentTime
                        isFingerDown = true

                        // Start a delayed handler to check for long press
                        handler.postDelayed({
                            if (isFingerDown && !isProcessingGesture) {
                                isProcessingGesture = true
                                showToast("Long press detected!")
                                handleLongPress()
                                resetAndRestartScanning()
                            }
                        }, longPressThreshold)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        isFingerDown = false
                        resetAndRestartScanning()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        if (!isProcessingGesture) {
                            isFingerDown = false
                            isProcessingGesture = true
                            showToast("Single tap detected!")
                            handleSingleTap()
                            resetAndRestartScanning()
                        }
                    }
                },
                handler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting fingerprint scanning", e)
            isScanning = false
            handler.postDelayed({ startFingerprintScanning() }, 1000)
        }
    }

    private fun stopFingerprintScanning() {
        cancellationSignal?.cancel()
        cancellationSignal = null
        isScanning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun resetGestureState() {
        tapCount = 0
        lastTapTime = 0L
        isProcessingGesture = false
        isFingerDown = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun handleSingleTap() {
        val action = preferencesManager.getAction(PreferencesManager.SINGLE_TAP_KEY) ?: PreferencesManager.Action.NO_ACTION
        if (action == PreferencesManager.Action.NO_ACTION) {
            showToast("Single tap - No action set")
            return
        }
        executeAction(action)
        showToast("Executing single tap action: ${action.displayName}")
    }

    private fun handleLongPress() {
        val action = preferencesManager.getAction(PreferencesManager.LONG_PRESS_KEY) ?: PreferencesManager.Action.NO_ACTION
        if (action == PreferencesManager.Action.NO_ACTION) {
            showToast("Long press - No action set")
            return
        }
        executeAction(action)
        showToast("Executing long press action: ${action.displayName}")
    }

    private fun executeAction(action: PreferencesManager.Action) {
        val intent = Intent(this, DeviceActionService::class.java).apply {
            this.action = "com.example.biometricactions.EXECUTE_ACTION"
            putExtra("action_id", action.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startService(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Not used for fingerprint gestures
    }

    override fun onInterrupt() {
        stopFingerprintScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenStateReceiver)
            stopShakeDetection()
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        stopFingerprintScanning()
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startShakeDetection() {
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    private fun stopShakeDetection() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            
            if ((currentTime - lastUpdate) > 100) { // Only process every 100ms
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                val speed = abs(x + y + z - lastX - lastY - lastZ) / (currentTime - lastUpdate) * 10000
                
                if (speed > SHAKE_THRESHOLD) {
                    if (!isShakeGestureInProgress) {
                        isShakeGestureInProgress = true
                        shakeCount = 1
                        lastShakeTime = currentTime
                    } else if ((currentTime - lastShakeTime) < SHAKE_TIMEOUT) {
                        shakeCount++
                        lastShakeTime = currentTime
                        
                        if (shakeCount >= REQUIRED_SHAKES) {
                            handleShakeGesture()
                            resetShakeDetection()
                        }
                    } else {
                        // Too much time passed, reset the count
                        resetShakeDetection()
                    }
                }
                
                lastX = x
                lastY = y
                lastZ = z
                lastUpdate = currentTime
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    private fun resetShakeDetection() {
        shakeCount = 0
        isShakeGestureInProgress = false
        lastShakeTime = 0L
    }

    private fun handleShakeGesture() {
        if (!isProcessingGesture) {
            isProcessingGesture = true
            showToast("Shake gesture detected!")
            val action = preferencesManager.getAction(PreferencesManager.SHAKE_KEY) ?: PreferencesManager.Action.NO_ACTION
            if (action == PreferencesManager.Action.NO_ACTION) {
                showToast("Shake - No action set")
                return
            }
            executeAction(action)
            showToast("Executing shake action: ${action.displayName}")
            resetAndRestartScanning()
        }
    }

    private fun resetAndRestartScanning() {
        handler.postDelayed({
            resetGestureState()
            startFingerprintScanning()
        }, 1000) // Increased delay between gestures
    }

    private fun createForegroundNotification(): Notification {
        val channelId = "biometric_actions_foreground"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Biometric Actions Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the service running in background"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Biometric Actions Active")
            .setContentText("Service is running in background")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val TAG = "BiometricService"
        private const val NOTIFICATION_ID = 1002
        private const val GESTURE_TIMEOUT = 500L // ms
        private const val SHAKE_THRESHOLD = 800 // Adjust this value to change shake sensitivity
        private const val SHAKE_TIMEOUT = 2000L // Time window for shakes in milliseconds
        private const val REQUIRED_SHAKES = 5 // Number of shakes required to trigger action
    }
} 