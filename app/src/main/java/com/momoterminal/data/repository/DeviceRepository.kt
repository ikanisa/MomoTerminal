package com.momoterminal.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.momoterminal.api.MomoApiService
import com.momoterminal.data.preferences.UserPreferences
import com.momoterminal.data.remote.dto.RegisterDeviceRequest
import com.momoterminal.util.DeviceInfoProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for device registration and management.
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val api: MomoApiService,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val userPreferences: UserPreferences
) {
    
    /**
     * Register this device with the backend.
     * Should be called after user authentication.
     */
    suspend fun registerDevice(): Result<String> {
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val fcmToken = getFcmToken()
            
            val request = RegisterDeviceRequest(
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
                deviceModel = deviceInfo.deviceModel,
                manufacturer = deviceInfo.manufacturer,
                osVersion = deviceInfo.osVersion,
                sdkVersion = deviceInfo.sdkVersion,
                appVersion = deviceInfo.appVersion,
                fcmToken = fcmToken
            )
            
            val response = api.registerDevice(request)
            
            // Save device UUID for future sync operations
            userPreferences.saveDeviceUuid(response.id)
            
            Timber.d("Device registered successfully: ${response.id}")
            Result.success(response.id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register device")
            Result.failure(e)
        }
    }
    
    /**
     * Update FCM token on the backend.
     */
    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val deviceUuid = userPreferences.getDeviceUuid()
            if (deviceUuid == null) {
                Timber.w("Device not registered, skipping FCM token update")
                return Result.success(Unit)
            }
            
            api.updateDeviceToken(deviceUuid, token)
            Timber.d("FCM token updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update FCM token")
            Result.failure(e)
        }
    }
    
    /**
     * Check if device is registered.
     */
    fun isDeviceRegistered(): Boolean {
        return userPreferences.getDeviceUuid() != null
    }
    
    /**
     * Get FCM token with error handling.
     */
    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get FCM token")
            null
        }
    }
}
