package com.example.biometricactions.util

import android.content.Context
import android.content.SharedPreferences

class AuthSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun isSessionValid(): Boolean {
        val lastAuthTime = prefs.getLong(KEY_LAST_AUTH_TIME, 0)
        return System.currentTimeMillis() - lastAuthTime < SESSION_DURATION
    }
    
    fun startSession() {
        prefs.edit().putLong(KEY_LAST_AUTH_TIME, System.currentTimeMillis()).apply()
    }
    
    fun clearSession() {
        prefs.edit().remove(KEY_LAST_AUTH_TIME).apply()
    }
    
    companion object {
        private const val PREF_NAME = "auth_session_prefs"
        private const val KEY_LAST_AUTH_TIME = "last_auth_time"
        // Session duration of 30 minutes
        private const val SESSION_DURATION = 30 * 60 * 1000L
    }
} 