package com.example.biometricactions.util

import android.content.Context
import android.content.SharedPreferences
import com.example.biometricactions.model.Action

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setAction(key: String, action: Action) {
        prefs.edit().putInt(key, action.id).apply()
    }

    fun getAction(key: String): Action? {
        val actionId = prefs.getInt(key, -1)
        return Action.values().find { it.id == actionId }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BIOMETRICS_ENABLED_KEY, enabled).apply()
    }

    fun isBiometricsEnabled(): Boolean {
        return prefs.getBoolean(BIOMETRICS_ENABLED_KEY, false)
    }

    companion object {
        const val PREFS_NAME = "BiometricActionsPrefs"
        const val BIOMETRICS_ENABLED_KEY = "biometrics_enabled"
        const val SINGLE_TAP_KEY = "single_tap_action"
        const val DOUBLE_TAP_KEY = "double_tap_action"
        const val LONG_PRESS_KEY = "long_press_action"
    }

    enum class Action(val id: Int, val displayName: String) {
        NO_ACTION(0, "No Action"),
        TOGGLE_FLASHLIGHT(1, "Toggle Flashlight"),
        OPEN_CAMERA(2, "Open Camera"),
        TOGGLE_SILENT_MODE(3, "Toggle Silent Mode"),
        TAKE_SCREENSHOT(4, "Take Screenshot");

        companion object {
            fun getDefaultAction(key: String): Action {
                return NO_ACTION
            }
        }
    }
} 