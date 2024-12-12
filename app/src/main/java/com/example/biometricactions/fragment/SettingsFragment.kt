package com.example.biometricactions.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.biometricactions.R
import com.example.biometricactions.service.BiometricAccessibilityService
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {
    private lateinit var switchAccessibility: SwitchMaterial
    private lateinit var buttonAccessibility: MaterialButton
    private lateinit var textAccessibilityStatus: TextView
    private lateinit var buttonLightTheme: MaterialButton
    private lateinit var buttonDarkTheme: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        switchAccessibility = view.findViewById(R.id.switchAccessibility)
        buttonAccessibility = view.findViewById(R.id.buttonAccessibility)
        textAccessibilityStatus = view.findViewById(R.id.textAccessibilityStatus)
        buttonLightTheme = view.findViewById(R.id.buttonLightTheme)
        buttonDarkTheme = view.findViewById(R.id.buttonDarkTheme)

        setupThemeButtons()
        setupAccessibilityControls()
    }

    private fun setupThemeButtons() {
        buttonLightTheme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            updateThemeButtons()
        }

        buttonDarkTheme.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            updateThemeButtons()
        }

        updateThemeButtons()
    }

    private fun updateThemeButtons() {
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        buttonLightTheme.isEnabled = !isNightMode
        buttonDarkTheme.isEnabled = isNightMode
    }

    private fun setupAccessibilityControls() {
        buttonAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        switchAccessibility.isChecked = isAccessibilityServiceEnabled()
        updateAccessibilityStatus()
    }

    private fun updateAccessibilityStatus() {
        val isEnabled = isAccessibilityServiceEnabled()
        textAccessibilityStatus.text = if (isEnabled) {
            "Accessibility service is enabled"
        } else {
            "Accessibility service is disabled"
        }
        textAccessibilityStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            0
        }

        if (accessibilityEnabled == 1) {
            val service = "${requireContext().packageName}/${BiometricAccessibilityService::class.java.name}"
            val settingValue = Settings.Secure.getString(
                requireContext().contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(service) == true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        switchAccessibility.isChecked = isAccessibilityServiceEnabled()
        updateAccessibilityStatus()
    }
} 