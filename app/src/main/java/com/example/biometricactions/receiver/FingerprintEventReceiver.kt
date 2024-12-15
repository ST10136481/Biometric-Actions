package com.example.biometricactions.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.biometricactions.service.DeviceActionService
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
                if (!isLongPress && pressDuration < LONG_PRESS_DURATION) {
                    handleAction(context, preferencesManager, ActionType.SINGLE_TAP)
                }
            }
        }

        // Log event to Firebase
        logEventToFirebase(event, currentTime)
    }

    private fun handleAction(context: Context, preferencesManager: PreferencesManager, actionType: ActionType) {
        val actionKey = when (actionType) {
            ActionType.SINGLE_TAP -> PreferencesManager.SINGLE_TAP_KEY
            ActionType.LONG_PRESS -> PreferencesManager.LONG_PRESS_KEY
        }

        val action = preferencesManager.getAction(actionKey)
        if (action == PreferencesManager.Action.NO_ACTION) {
            showToast(context, "${actionType.name.lowercase()} - No action set")
            return
        }
        
        // Send intent to DeviceActionService to execute the action
        val actionIntent = Intent(context, DeviceActionService::class.java).apply {
            this.action = "com.example.biometricactions.EXECUTE_ACTION"
            putExtra("action_id", action?.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startService(actionIntent)
        showToast(context, "Executing ${actionType.name.lowercase()} action: ${action?.displayName}")
    }

    private fun showToast(context: Context, message: String) {
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
    }

    enum class ActionType {
        SINGLE_TAP,
        LONG_PRESS
    }
} 