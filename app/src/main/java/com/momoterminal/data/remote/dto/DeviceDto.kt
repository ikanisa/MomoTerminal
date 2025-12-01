package com.momoterminal.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request to register a new device.
 */
@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "device_name") val deviceName: String,
    @Json(name = "device_model") val deviceModel: String,
    @Json(name = "manufacturer") val manufacturer: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "sdk_version") val sdkVersion: Int,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "fcm_token") val fcmToken: String?
)

/**
 * Response from device registration.
 */
@JsonClass(generateAdapter = true)
data class RegisterDeviceResponse(
    @Json(name = "id") val id: String,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "is_trusted") val isTrusted: Boolean,
    @Json(name = "registered_at") val registeredAt: String
)

/**
 * Request to update FCM token.
 */
@JsonClass(generateAdapter = true)
data class UpdateFcmTokenRequest(
    @Json(name = "fcm_token") val fcmToken: String
)

/**
 * Analytics event for batch logging.
 */
@JsonClass(generateAdapter = true)
data class AnalyticsEventDto(
    @Json(name = "event_name") val eventName: String,
    @Json(name = "event_category") val eventCategory: String? = null,
    @Json(name = "event_action") val eventAction: String? = null,
    @Json(name = "event_label") val eventLabel: String? = null,
    @Json(name = "event_value") val eventValue: Double? = null,
    @Json(name = "event_properties") val eventProperties: Map<String, Any>? = null,
    @Json(name = "screen_name") val screenName: String? = null,
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "timestamp") val timestamp: String
)

/**
 * Batch analytics events request.
 */
@JsonClass(generateAdapter = true)
data class BatchAnalyticsRequest(
    @Json(name = "events") val events: List<AnalyticsEventDto>,
    @Json(name = "device_id") val deviceId: String
)

/**
 * Error log for reporting.
 */
@JsonClass(generateAdapter = true)
data class ErrorLogDto(
    @Json(name = "error_type") val errorType: String,
    @Json(name = "error_code") val errorCode: String? = null,
    @Json(name = "severity") val severity: String,
    @Json(name = "error_message") val errorMessage: String,
    @Json(name = "stack_trace") val stackTrace: String? = null,
    @Json(name = "error_context") val errorContext: Map<String, Any>? = null,
    @Json(name = "component") val component: String? = null,
    @Json(name = "function_name") val functionName: String? = null,
    @Json(name = "screen_name") val screenName: String? = null,
    @Json(name = "user_action") val userAction: String? = null,
    @Json(name = "device_id") val deviceId: String,
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "os_version") val osVersion: String,
    @Json(name = "timestamp") val timestamp: String
)

/**
 * Batch error logs request.
 */
@JsonClass(generateAdapter = true)
data class BatchErrorLogsRequest(
    @Json(name = "errors") val errors: List<ErrorLogDto>
)

/**
 * Merchant settings request.
 */
@JsonClass(generateAdapter = true)
data class MerchantSettingsDto(
    @Json(name = "business_name") val businessName: String?,
    @Json(name = "business_type") val businessType: String?,
    @Json(name = "merchant_code") val merchantCode: String?,
    @Json(name = "business_phone") val businessPhone: String?,
    @Json(name = "business_email") val businessEmail: String?,
    @Json(name = "preferred_provider") val preferredProvider: String,
    @Json(name = "enabled_providers") val enabledProviders: List<String>,
    @Json(name = "nfc_enabled") val nfcEnabled: Boolean,
    @Json(name = "auto_sync_enabled") val autoSyncEnabled: Boolean,
    @Json(name = "push_notifications") val pushNotifications: Boolean,
    @Json(name = "email_notifications") val emailNotifications: Boolean
)
