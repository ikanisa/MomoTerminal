package com.momoterminal.core.data.repository

// import com.google.firebase.messaging.FirebaseMessaging
import com.momoterminal.core.network.api.MomoApiService
import com.momoterminal.core.common.preferences.UserPreferences
import com.momoterminal.core.network.dto.RegisterDeviceRequest
import com.momoterminal.core.common.DeviceInfoProvider
// import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for device registration and management.
 * TODO: Implement when backend endpoints are ready
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
     * TODO: Uncomment when registerDevice API endpoint is implemented
     */
    suspend fun registerDevice(): Result<String> {
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            // val fcmToken = getFcmToken()
            
            // TODO: Uncomment when API is ready
            /*
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
            */
            
            // Temporary: Return device ID as UUID
            userPreferences.saveDeviceUuid(deviceInfo.deviceId)
            Timber.d("Device registered (local only): ${deviceInfo.deviceId}")
            Result.success(deviceInfo.deviceId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register device")
            Result.failure(e)
        }
    }
    
    /**
     * Update FCM token on the backend.
     * TODO: Uncomment when updateDeviceToken API endpoint is implemented
     */
    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val deviceUuid = userPreferences.getDeviceUuid()
            if (deviceUuid == null) {
                Timber.w("Device not registered, skipping FCM token update")
                return Result.success(Unit)
            }
            
            // TODO: Uncomment when API is ready
            // api.updateDeviceToken(deviceUuid, token)
            Timber.d("FCM token update skipped (not implemented)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update FCM token")
            Result.failure(e)
        }
    }
    
    /**
     * Check if device is registered.
     */
    suspend fun isDeviceRegistered(): Boolean {
        return userPreferences.getDeviceUuid() != null
    }
    
    /**
     * Get FCM token with error handling.
     * TODO: Uncomment when Firebase is integrated
     */
    /*
    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get FCM token")
            null
        }
    }
    */
}
