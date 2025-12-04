package com.momoterminal.core.common

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides device information for registration and tracking.
 */
@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val deviceModel: String,
        val manufacturer: String,
        val osVersion: String,
        val sdkVersion: Int,
        val appVersion: String
    )
    
    /**
     * Get current device information.
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = getDeviceId(),
            deviceName = getDeviceName(),
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            appVersion = getAppVersion()
        )
    }
    
    /**
     * Get unique device identifier.
     * Uses Android ID which persists across app reinstalls but changes on factory reset.
     */
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
    
    /**
     * Get user-friendly device name.
     */
    private fun getDeviceName(): String {
        return Settings.Global.getString(
            context.contentResolver,
            Settings.Global.DEVICE_NAME
        ) ?: "${Build.MANUFACTURER} ${Build.MODEL}"
    }
    
    /**
     * Get app version from package info.
     */
    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
