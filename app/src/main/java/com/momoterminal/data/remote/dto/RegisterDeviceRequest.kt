package com.momoterminal.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request payload for device registration.
 * Used when registering this device with the backend server.
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
    val fcmToken: String?
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
    val status: String
)
