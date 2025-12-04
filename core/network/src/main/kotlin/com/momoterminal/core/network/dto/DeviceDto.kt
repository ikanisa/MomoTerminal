package com.momoterminal.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for device registration and management.
 * Used for backend API communication.
 */

/**
 * Request payload for device registration.
 */
data class RegisterDeviceRequest(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("device_name")
    val deviceName: String,
    
    @SerializedName("device_model")
    val deviceModel: String,
    
    @SerializedName("manufacturer")
    val manufacturer: String,
    
    @SerializedName("os_version")
    val osVersion: String,
    
    @SerializedName("sdk_version")
    val sdkVersion: Int,
    
    @SerializedName("app_version")
    val appVersion: String,
    
    @SerializedName("fcm_token")
    val fcmToken: String? = null
)

/**
 * Response from device registration endpoint.
 */
data class RegisterDeviceResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Request payload for updating device FCM token.
 */
data class UpdateDeviceTokenRequest(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("fcm_token")
    val fcmToken: String
)

/**
 * Response from update device token endpoint.
 */
data class UpdateDeviceTokenResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String
)
