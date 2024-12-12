package com.example.biometricactions

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.core.content.ContextCompat
import com.example.biometricactions.fragment.SettingsFragment
import com.example.biometricactions.service.BiometricAccessibilityService
import com.example.biometricactions.HomeFragment

class MainActivity : AppCompatActivity() {
    private lateinit var biometricManager: BiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        biometricManager = BiometricManager.from(this)
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                if (isAccessibilityServiceEnabled()) {
                    showHomeFragment()
                } else {
                    showAccessibilitySetup()
                }
            }
            else -> {
                showBiometricErrorDialog()
            }
        }
    }

    private fun showBiometricErrorDialog() {
        // Add blur effect to background
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.scrim)))

        AlertDialog.Builder(this)
            .setTitle("Biometric Authentication Required")
            .setMessage("This app requires fingerprint authentication. Please set up fingerprint authentication in your device settings.")
            .setCancelable(false)
            .show()

        // Close app after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 5000)
    }

    private fun showHomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }

    private fun showAccessibilitySetup() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SettingsFragment())
            .commit()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled == 1) {
            val service = "${packageName}/${BiometricAccessibilityService::class.java.name}"
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(service) == true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityServiceEnabled()) {
            showHomeFragment()
        }
    }
}