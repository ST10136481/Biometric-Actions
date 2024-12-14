package com.example.biometricactions.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.IBinder
import android.widget.Toast
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
                else -> showToast("Unknown action")
            }
        }
        return START_NOT_STICKY
    }

    private fun toggleFlashlight() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
            showToast("Flashlight ${if (isFlashlightOn) "on" else "off"}")
        } catch (e: Exception) {
            showToast("Failed to toggle flashlight")
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
            showToast("Ringer mode: ${if (newMode == AudioManager.RINGER_MODE_SILENT) "Silent" else "Normal"}")
        } catch (e: Exception) {
            showToast("Failed to toggle silent mode")
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent("android.media.action.STILL_IMAGE_CAMERA").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            showToast("Opening camera")
        } catch (e: Exception) {
            showToast("Failed to open camera")
        }
    }

    private fun takeScreenshot() {
        showToast("Screenshot not implemented")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null
} 