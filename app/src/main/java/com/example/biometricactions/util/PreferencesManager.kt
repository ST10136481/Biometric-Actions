package com.example.biometricactions.util

import android.content.Context
import android.content.SharedPreferences
import com.example.biometricactions.model.Action

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, enabled).apply()
    }

    fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRICS_ENABLED, false)
    }

    fun setAction(key: String, action: Action) {
        prefs.edit().putInt(key, action.id).apply()
    }

    fun getAction(key: String): Action? {
        val actionId = prefs.getInt(key, -1)
        return Action.values().find { it.id == actionId }
    }

    enum class Action(val id: Int, val displayName: String) {
        NO_ACTION(0, "No Action"),
        TOGGLE_FLASHLIGHT(1, "Toggle Flashlight"),
        OPEN_CAMERA(2, "Open Camera"),
        TOGGLE_SILENT_MODE(3, "Toggle Silent Mode"),
        TAKE_SCREENSHOT(4, "Take Screenshot")
    }

    companion object {
        private const val PREFS_NAME = "biometric_actions_prefs"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        const val SINGLE_TAP_KEY = "single_tap_action"
        const val SHAKE_KEY = "shake_action"
        const val LONG_PRESS_KEY = "long_press_action"
    }
} 