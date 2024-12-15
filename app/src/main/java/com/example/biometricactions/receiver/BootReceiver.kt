package com.example.biometricactions.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.example.biometricactions.service.BiometricAccessibilityService
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, ACTION_QUICKBOOT_POWERON -> {
                Log.d(TAG, "Boot completed, checking accessibility service")
                
                // Check if our accessibility service is enabled
                val isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
                
                if (!isAccessibilityEnabled) {
                    // Create notification to prompt user to enable accessibility service
                    createServiceNotification(context)
                } else {
                    // Start the accessibility service
                    startAccessibilityService(context)
                }
            }
        }
    }

    private fun startAccessibilityService(context: Context) {
        val serviceIntent = Intent(context, BiometricAccessibilityService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun createServiceNotification(context: Context) {
        val channelId = "biometric_actions_service"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Biometric Actions Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Biometric Actions service status"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open accessibility settings
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Biometric Actions Service Disabled")
            .setContentText("Tap to enable the accessibility service")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled == 1) {
            val serviceString = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            return serviceString.contains(
                context.packageName + "/" + BiometricAccessibilityService::class.java.name
            )
        }
        return false
    }

    companion object {
        private const val TAG = "BootReceiver"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
} 