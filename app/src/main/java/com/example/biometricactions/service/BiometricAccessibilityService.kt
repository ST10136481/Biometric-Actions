package com.example.biometricactions.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.hardware.fingerprint.FingerprintManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.biometricactions.util.PreferencesManager

class BiometricAccessibilityService : AccessibilityService() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var fingerprintManager: FingerprintManager
    private var cancellationSignal: CancellationSignal? = null
    private var isScanning = false
    private var lastTapTime = 0L
    private val doubleTapInterval = 500L
    private val handler = Handler(Looper.getMainLooper())
    private var pendingEvent: EventType? = null

    enum class EventType {
        SingleTap,
        FastSwipe,
        DoubleTap,
        Unregistered
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
        
        preferencesManager = PreferencesManager(this)
        fingerprintManager = getSystemService(FingerprintManager::class.java)
        
        if (preferencesManager.isBiometricsEnabled()) {
            startFingerprintScanning()
        }
        Log.d(TAG, "BiometricAccessibilityService connected")
        showToast("Fingerprint Service Connected")
    }

    private fun startFingerprintScanning() {
        if (!preferencesManager.isBiometricsEnabled()) {
            Log.d(TAG, "Biometrics not enabled, skipping scan start")
            return
        }

        if (isScanning) {
            stopFingerprintScanning()
        }

        cancellationSignal = CancellationSignal()
        isScanning = true
        
        try {
            fingerprintManager.authenticate(
                null,  // No crypto object needed
                cancellationSignal,
                0,  // No flags
                authenticationCallback,
                handler  // Use handler for callbacks
            )
            Log.d(TAG, "Started fingerprint scanning")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting fingerprint scanning", e)
            isScanning = false
        }
    }

    private fun stopFingerprintScanning() {
        cancellationSignal?.cancel()
        cancellationSignal = null
        isScanning = false
    }

    private val authenticationCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            Log.d(TAG, "Authentication succeeded")
            handler.post {
                onEvent(EventType.SingleTap)
                // Restart scanning after a short delay
                handler.postDelayed({ startFingerprintScanning() }, 500)
            }
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)
            Log.d(TAG, "Authentication help: $helpString")
            handler.post {
                onEvent(EventType.FastSwipe)
                // Restart scanning after a short delay
                handler.postDelayed({ startFingerprintScanning() }, 500)
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.d(TAG, "Authentication failed")
            handler.post {
                onEvent(EventType.Unregistered)
                // Restart scanning immediately for failed attempts
                startFingerprintScanning()
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            Log.d(TAG, "Authentication error: $errString")
            isScanning = false
            // Restart scanning after error with a delay
            handler.postDelayed({ startFingerprintScanning() }, 1000)
        }
    }

    private fun onEvent(event: EventType) {
        Log.d(TAG, "Event detected: ${event.name}")

        if (!preferencesManager.isBiometricsEnabled()) {
            showToast("Biometrics not enabled")
            return
        }

        val currentTime = System.currentTimeMillis()
        
        when (event) {
            EventType.SingleTap -> {
                if (pendingEvent == null) {
                    pendingEvent = event
                    handler.postDelayed({
                        if (pendingEvent == EventType.SingleTap) {
                            handleSingleTap()
                            pendingEvent = null
                        }
                    }, doubleTapInterval)
                } else if (pendingEvent == EventType.SingleTap && 
                         currentTime - lastTapTime < doubleTapInterval) {
                    // Double tap detected
                    pendingEvent = null
                    handler.removeCallbacksAndMessages(null)
                    handleDoubleTap()
                }
                lastTapTime = currentTime
            }
            EventType.FastSwipe -> {
                handleLongPress()
            }
            else -> {
                // Handle other events if needed
            }
        }
    }

    private fun handleSingleTap() {
        val action = preferencesManager.getAction(PreferencesManager.SINGLE_TAP_KEY)
        action?.let { 
            executeAction(it)
            showToast("Executing Single Tap Action: ${it.displayName}")
        } ?: showToast("No action set for Single Tap")
    }

    private fun handleDoubleTap() {
        val action = preferencesManager.getAction(PreferencesManager.DOUBLE_TAP_KEY)
        action?.let { 
            executeAction(it)
            showToast("Executing Double Tap Action: ${it.displayName}")
        } ?: showToast("No action set for Double Tap")
    }

    private fun handleLongPress() {
        val action = preferencesManager.getAction(PreferencesManager.LONG_PRESS_KEY)
        action?.let { 
            executeAction(it)
            showToast("Executing Long Press Action: ${it.displayName}")
        } ?: showToast("No action set for Long Press")
    }

    private fun executeAction(action: PreferencesManager.Action) {
        val intent = Intent(this, DeviceActionService::class.java).apply {
            this.action = "com.example.biometricactions.EXECUTE_ACTION"
            putExtra("action_id", action.id)
        }
        startService(intent)
        Log.d(TAG, "Executing action: ${action.displayName}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Not used for fingerprint gestures
    }

    override fun onInterrupt() {
        Log.d(TAG, "BiometricAccessibilityService interrupted")
        stopFingerprintScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFingerprintScanning()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "BiometricAccessibility"
    }
} 