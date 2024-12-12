package com.example.biometricactions.xposed

import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FingerprintHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android" || lpparam.packageName == "com.android.systemui") {
            hookFingerprintService(lpparam)
        }
    }

    private fun hookFingerprintService(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Hook the FingerprintService class
            val fingerprintServiceClass = XposedHelpers.findClass(
                "com.android.server.biometrics.fingerprint.FingerprintService",
                lpparam.classLoader
            )

            // Hook the onFingerDown method
            XposedHelpers.findAndHookMethod(
                fingerprintServiceClass,
                "onFingerDown",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Called when finger touches the sensor
                        XposedBridge.log("Finger touched sensor")
                        notifyFingerprintEvent("FINGER_DOWN")
                    }
                }
            )

            // Hook the onFingerUp method
            XposedHelpers.findAndHookMethod(
                fingerprintServiceClass,
                "onFingerUp",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Called when finger is removed from sensor
                        XposedBridge.log("Finger removed from sensor")
                        notifyFingerprintEvent("FINGER_UP")
                    }
                }
            )

            // Hook authentication results
            XposedHelpers.findAndHookMethod(
                fingerprintServiceClass,
                "handleAuthenticated",
                Long::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val fingerId = param.args[0] as Long
                        val userId = param.args[1] as Int
                        XposedBridge.log("Fingerprint authenticated: $fingerId for user $userId")
                        notifyFingerprintEvent("AUTHENTICATED")
                    }
                }
            )
        } catch (e: Throwable) {
            XposedBridge.log("Error in FingerprintHook: ${e.message}")
        }
    }

    private fun notifyFingerprintEvent(event: String) {
        // Send broadcast to our app
        val intent = android.content.Intent("com.example.biometricactions.FINGERPRINT_EVENT")
        intent.putExtra("event", event)
        android.app.AndroidAppHelper.currentApplication().sendBroadcast(intent)
    }
} 