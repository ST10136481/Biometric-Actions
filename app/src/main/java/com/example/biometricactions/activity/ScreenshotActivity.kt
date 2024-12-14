package com.example.biometricactions.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.biometricactions.service.DeviceActionService

class ScreenshotActivity : Activity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager

    companion object {
        private const val REQ_CREATE_SCREEN_CAPTURE = 0
        private const val REQ_STORAGE_PERMISSION = 1
        
        fun startActivity(context: Context) {
            val startIntent = Intent(context, ScreenshotActivity::class.java)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            context.startActivity(startIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // Check storage permission
        if (checkStoragePermission()) {
            requestScreenCapture()
        } else {
            requestStoragePermission()
        }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQ_STORAGE_PERMISSION
        )
    }

    private fun requestScreenCapture() {
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, REQ_CREATE_SCREEN_CAPTURE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQ_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestScreenCapture()
            } else {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_CREATE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                // Send the result back to DeviceActionService
                val serviceIntent = Intent(this, DeviceActionService::class.java).apply {
                    action = "com.example.biometricactions.SCREENSHOT_RESULT"
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                startService(serviceIntent)
            }
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
} 