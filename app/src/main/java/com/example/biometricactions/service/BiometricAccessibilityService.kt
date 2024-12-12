package com.example.biometricactions.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.FingerprintGestureController
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.biometricactions.util.PreferencesManager

class BiometricAccessibilityService : AccessibilityService() {
    private lateinit var preferencesManager: PreferencesManager
    private var fingerprintGestureController: FingerprintGestureController? = null

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
            notificationTimeout = 100
        }
        serviceInfo = info
        
        fingerprintGestureController = getFingerprintGestureController()
        preferencesManager = PreferencesManager(this)
        
        Log.d(TAG, "BiometricAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Not used for fingerprint gestures
    }

    override fun onInterrupt() {
        Log.d(TAG, "BiometricAccessibilityService interrupted")
    }

    override fun onGesture(gestureId: Int): Boolean {
        if (!preferencesManager.isBiometricsEnabled()) return false

        Log.d(TAG, "Gesture detected: $gestureId")
        
        when (gestureId) {
            GESTURE_FINGERPRINT_SWIPE_UP -> {
                Log.d(TAG, "Swipe Up detected")
                handleSingleTap()
                return true
            }
            GESTURE_FINGERPRINT_SWIPE_DOWN -> {
                Log.d(TAG, "Swipe Down detected")
                handleDoubleTap()
                return true
            }
            GESTURE_FINGERPRINT_SWIPE_LEFT, GESTURE_FINGERPRINT_SWIPE_RIGHT -> {
                Log.d(TAG, "Swipe Left/Right detected")
                handleLongPress()
                return true
            }
        }
        return false
    }

    private fun handleSingleTap() {
        val action = preferencesManager.getAction(PreferencesManager.SINGLE_TAP_KEY)
        action?.let { executeAction(it) }
    }

    private fun handleDoubleTap() {
        val action = preferencesManager.getAction(PreferencesManager.DOUBLE_TAP_KEY)
        action?.let { executeAction(it) }
    }

    private fun handleLongPress() {
        val action = preferencesManager.getAction(PreferencesManager.LONG_PRESS_KEY)
        action?.let { executeAction(it) }
    }

    private fun executeAction(action: PreferencesManager.Action) {
        val intent = Intent(this, DeviceActionService::class.java).apply {
            this.action = "com.example.biometricactions.EXECUTE_ACTION"
            putExtra("action_id", action.id)
        }
        startService(intent)
        Log.d(TAG, "Executing action: ${action.displayName}")
    }

    companion object {
        private const val TAG = "BiometricAccessibility"
        private const val GESTURE_FINGERPRINT_SWIPE_UP = 8
        private const val GESTURE_FINGERPRINT_SWIPE_DOWN = 4
        private const val GESTURE_FINGERPRINT_SWIPE_LEFT = 2
        private const val GESTURE_FINGERPRINT_SWIPE_RIGHT = 1
    }
} 