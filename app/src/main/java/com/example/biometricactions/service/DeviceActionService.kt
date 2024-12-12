package com.example.biometricactions.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import com.example.biometricactions.util.PreferencesManager

class DeviceActionService : Service() {
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private var isFlashlightOn = false

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.example.biometricactions.EXECUTE_ACTION") {
            val actionId = intent.getIntExtra("action_id", -1)
            val action = PreferencesManager.Action.values().find { it.id == actionId }
            
            when (action) {
                PreferencesManager.Action.TOGGLE_FLASHLIGHT -> toggleFlashlight()
                PreferencesManager.Action.TOGGLE_SILENT_MODE -> toggleSilentMode()
                PreferencesManager.Action.OPEN_CAMERA -> openCamera()
                PreferencesManager.Action.TAKE_SCREENSHOT -> takeScreenshot()
                else -> Log.d(TAG, "Unknown action ID: $actionId")
            }
        }
        return START_NOT_STICKY
    }

    private fun toggleFlashlight() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            Log.d(TAG, "Flashlight ${if (isFlashlightOn) "on" else "off"}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flashlight", e)
        }
    }

    private fun toggleSilentMode() {
        try {
            val currentMode = audioManager.ringerMode
            val newMode = if (currentMode == AudioManager.RINGER_MODE_NORMAL) {
                AudioManager.RINGER_MODE_SILENT
            } else {
                AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = newMode
            Log.d(TAG, "Ringer mode changed to: $newMode")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling silent mode", e)
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent("android.media.action.STILL_IMAGE_CAMERA")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d(TAG, "Camera opened")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
        }
    }

    private fun takeScreenshot() {
        // This requires root access or system permissions
        // For now, just log that it's not implemented
        Log.d(TAG, "Screenshot functionality not implemented")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "DeviceActionService"
    }
} 