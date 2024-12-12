package com.example.biometricactions.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.biometricactions.util.PreferencesManager
import com.google.firebase.firestore.FirebaseFirestore

class FingerprintEventReceiver : BroadcastReceiver() {
    private var lastDownTime: Long = 0
    private var isLongPress = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onReceive(context: Context, intent: Intent) {
        val event = intent.getStringExtra("event") ?: return
        val currentTime = System.currentTimeMillis()
        val preferencesManager = PreferencesManager(context)

        Log.d(TAG, "Received fingerprint event: $event")

        when (event) {
            "FINGER_DOWN" -> {
                lastDownTime = currentTime
                // Start long press detection
                isLongPress = false
                handler.postDelayed({
                    if (currentTime - lastDownTime >= LONG_PRESS_DURATION) {
                        isLongPress = true
                        handleAction(context, preferencesManager, ActionType.LONG_PRESS)
                    }
                }, LONG_PRESS_DURATION)
            }
            "FINGER_UP" -> {
                val pressDuration = currentTime - lastDownTime
                if (!isLongPress) {
                    if (pressDuration < DOUBLE_TAP_WINDOW && lastDownTime - lastTapTime < DOUBLE_TAP_WINDOW) {
                        // Double tap detected
                        handleAction(context, preferencesManager, ActionType.DOUBLE_TAP)
                        lastTapTime = 0
                    } else {
                        // Single tap detected
                        handleAction(context, preferencesManager, ActionType.SINGLE_TAP)
                        lastTapTime = currentTime
                    }
                }
            }
        }

        // Log event to Firebase
        logEventToFirebase(event, currentTime)
    }

    private fun handleAction(context: Context, preferencesManager: PreferencesManager, actionType: ActionType) {
        val actionKey = when (actionType) {
            ActionType.SINGLE_TAP -> PreferencesManager.SINGLE_TAP_KEY
            ActionType.DOUBLE_TAP -> PreferencesManager.DOUBLE_TAP_KEY
            ActionType.LONG_PRESS -> PreferencesManager.LONG_PRESS_KEY
        }

        val action = preferencesManager.getAction(actionKey)
        action?.let {
            // Send broadcast to BiometricAccessibilityService to execute the action
            val actionIntent = Intent("com.example.biometricactions.EXECUTE_ACTION")
            actionIntent.putExtra("action_id", it.id)
            context.sendBroadcast(actionIntent)
        }
    }

    private fun logEventToFirebase(event: String, timestamp: Long) {
        val db = FirebaseFirestore.getInstance()
        val eventMap = hashMapOf(
            "event" to event,
            "timestamp" to timestamp
        )

        db.collection("fingerprint_events")
            .add(eventMap)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Event logged to Firebase with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error logging event to Firebase", e)
            }
    }

    companion object {
        private const val TAG = "FingerprintEventReceiver"
        private const val LONG_PRESS_DURATION = 500L
        private const val DOUBLE_TAP_WINDOW = 300L
        private var lastTapTime: Long = 0
    }

    enum class ActionType {
        SINGLE_TAP,
        DOUBLE_TAP,
        LONG_PRESS
    }
} 