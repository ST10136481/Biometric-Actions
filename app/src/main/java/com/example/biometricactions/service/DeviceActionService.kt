package com.example.biometricactions.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.IBinder
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.media.projection.MediaProjectionManager
import android.media.projection.MediaProjection
import android.graphics.Point
import android.media.ImageReader
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Environment
import android.view.WindowManager
import com.example.biometricactions.activity.ScreenshotActivity
import com.example.biometricactions.util.PreferencesManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DeviceActionService : Service() {
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var windowManager: WindowManager
    private var isFlashlightOn = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "com.example.biometricactions.EXECUTE_ACTION" -> {
                val actionId = intent.getIntExtra("action_id", -1)
                val action = PreferencesManager.Action.values().find { it.id == actionId }
                
                when (action) {
                    PreferencesManager.Action.TOGGLE_FLASHLIGHT -> toggleFlashlight()
                    PreferencesManager.Action.TOGGLE_SILENT_MODE -> toggleSilentMode()
                    PreferencesManager.Action.OPEN_CAMERA -> openCamera()
                    PreferencesManager.Action.TAKE_SCREENSHOT -> initiateScreenshot()
                    else -> showToast("Unknown action")
                }
            }
            "com.example.biometricactions.SCREENSHOT_RESULT" -> {
                val resultCode = intent.getIntExtra("resultCode", 0)
                val data = intent.getParcelableExtra<Intent>("data")
                if (data != null) {
                    takeScreenshot(resultCode, data)
                }
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
            // Check if we have permission to change notification policy
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // Open notification policy access settings
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                showToast("Please grant Do Not Disturb access")
                return
            }

            // Get current ringer mode
            val currentMode = audioManager.ringerMode
            
            // Get max volume for ring and notification
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val defaultVolume = maxVolume / 2

            when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    // Set to silent mode
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    
                    // Ensure ring and notification volumes are 0
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                    
                    showToast("Silent mode enabled")
                }
                AudioManager.RINGER_MODE_SILENT -> {
                    // Set to vibrate mode
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    
                    // Keep volumes at 0 but enable vibration
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                    
                    showToast("Vibrate mode enabled")
                }
                AudioManager.RINGER_MODE_VIBRATE -> {
                    // Set to normal mode
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    
                    // Set default volumes for ring and notification
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, defaultVolume, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, defaultVolume, 0)
                    
                    showToast("Normal mode enabled")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to toggle silent mode: ${e.message}")
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

    private fun initiateScreenshot() {
        ScreenshotActivity.startActivity(this)
    }

    private fun takeScreenshot(resultCode: Int, data: Intent) {
        try {
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            val point = Point()
            windowManager.defaultDisplay.getRealSize(point)
            
            val imageReader = ImageReader.newInstance(
                point.x, point.y,
                PixelFormat.RGBA_8888, 2
            )

            val virtualDisplay = mediaProjection.createVirtualDisplay(
                "screenshot",
                point.x, point.y,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                imageReader.surface,
                null, null
            )

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * point.x
                
                var bitmap = Bitmap.createBitmap(
                    point.x + rowPadding / pixelStride,
                    point.y,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                image.close()
                reader.close()
                virtualDisplay.release()
                mediaProjection.stop()

                if (bitmap.width > point.x) {
                    val raw = bitmap
                    bitmap = Bitmap.createBitmap(raw, 0, 0, point.x, point.y)
                    raw.recycle()
                }

                // Save the screenshot
                val formatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                val timeStr = formatter.format(Date())
                val screenshotsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Screenshots"
                )
                screenshotsDir.mkdirs()
                
                val screenshotFile = File(screenshotsDir, "Screenshot_$timeStr.png")
                FileOutputStream(screenshotFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Notify media scanner
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(screenshotFile.toString()),
                    arrayOf("image/png"),
                    null
                )

                showToast("Screenshot saved")
                bitmap.recycle()
            }, handler)

        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to take screenshot: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
} 