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
    private var tapCount = 0
    private val doubleTapInterval = 500L // Increased time window for double tap
    private val longPressThreshold = 1000L // Increased time for long press
    private val handler = Handler(Looper.getMainLooper())
    private var isProcessingGesture = false

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
        
        preferencesManager.setBiometricsEnabled(true)
        startFingerprintScanning()
    }

    private fun startFingerprintScanning() {
        if (isScanning) {
            stopFingerprintScanning()
        }

        cancellationSignal = CancellationSignal()
        isScanning = true
        
        try {
            fingerprintManager.authenticate(
                null,
                cancellationSignal,
                0,
                authenticationCallback,
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
        handler.removeCallbacksAndMessages(null)
    }

    private val authenticationCallback = object : FingerprintManager.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
            super.onAuthenticationSucceeded(result)
            
            if (isProcessingGesture) {
                return
            }

            val currentTime = System.currentTimeMillis()
            
            // Check for long press
            if (lastTapTime > 0 && (currentTime - lastTapTime) >= longPressThreshold) {
                isProcessingGesture = true
                showToast("Long press detected!")
                handleLongPress()
                resetAndRestartScanning()
                return
            }

            // Increment tap count
            tapCount++
            
            if (tapCount == 1) {
                // First tap - start timer for potential double tap
                lastTapTime = currentTime
                handler.postDelayed({
                    if (tapCount == 1 && !isProcessingGesture) {
                        // No second tap came, handle as single tap
                        isProcessingGesture = true
                        showToast("Single tap detected!")
                        handleSingleTap()
                        resetAndRestartScanning()
                    }
                }, doubleTapInterval)
            } else if (tapCount == 2) {
                // Check if second tap is within double tap interval
                if ((currentTime - lastTapTime) < doubleTapInterval) {
                    isProcessingGesture = true
                    showToast("Double tap detected!")
                    handleDoubleTap()
                } else {
                    // Too slow for double tap, treat as new single tap
                    tapCount = 1
                    lastTapTime = currentTime
                }
                resetAndRestartScanning()
            }
        }

        override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {
            super.onAuthenticationHelp(helpCode, helpString)
            if (!isProcessingGesture) {
                isProcessingGesture = true
                showToast("Swipe detected!")
                handler.postDelayed({
                    handleLongPress()
                    resetAndRestartScanning()
                }, 200)
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            if (!isProcessingGesture) {
                resetAndRestartScanning()
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            super.onAuthenticationError(errorCode, errString)
            resetAndRestartScanning()
        }
    }

    private fun resetAndRestartScanning() {
        handler.postDelayed({
            resetGestureState()
            startFingerprintScanning()
        }, 1000) // Increased delay between gestures
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

    private fun handleDoubleTap() {
        val action = preferencesManager.getAction(PreferencesManager.DOUBLE_TAP_KEY) ?: PreferencesManager.Action.NO_ACTION
        if (action == PreferencesManager.Action.NO_ACTION) {
            showToast("Double tap - No action set")
            return
        }
        executeAction(action)
        showToast("Executing double tap action: ${action.displayName}")
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
        stopFingerprintScanning()
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "BiometricAccessibility"
    }
} 