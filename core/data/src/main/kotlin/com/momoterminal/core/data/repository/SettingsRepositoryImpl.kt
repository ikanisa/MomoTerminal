package com.momoterminal.core.data.repository

import com.momoterminal.core.data.mapper.SettingsMapper
import com.momoterminal.core.domain.model.settings.*
import com.momoterminal.core.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc.call
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : SettingsRepository {
    
    override suspend fun getMerchantSettings(userId: String): Result<MerchantSettings> = 
        withContext(Dispatchers.IO) {
            try {
                val params = buildJsonObject {
                    put("p_user_id", userId)
                }
                
                val response = supabaseClient.postgrest.rpc(
                    function = "get_merchant_settings",
                    parameters = params
                ).call<JsonObject>()
                
                val settings = SettingsMapper.jsonToMerchantSettings(response)
                
                if (settings != null) {
                    Result.success(settings)
                } else {
                    Result.failure(Exception("Failed to parse settings"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching merchant settings")
                Result.failure(e)
            }
        }
    
    override fun observeMerchantSettings(userId: String): Flow<MerchantSettings> = flow {
        getMerchantSettings(userId).getOrNull()?.let { emit(it) }
    }
    
    override suspend fun updateProfile(
        userId: String,
        businessName: String?,
        status: MerchantStatus?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                businessName?.let { put("p_business_name", it) }
                status?.let { put("p_status", it.name.lowercase()) }
            }
            
            supabaseClient.postgrest.rpc(
                function = "update_merchant_profile",
                parameters = params
            ).call<Boolean>()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating merchant profile")
            Result.failure(e)
        }
    }
    
    override suspend fun updateBusinessDetails(
        userId: String,
        details: BusinessDetails
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                details.businessType?.let { put("p_business_type", it.name.lowercase()) }
                details.taxId?.let { put("p_tax_id", it) }
                details.registrationNumber?.let { put("p_registration_number", it) }
                details.location?.let { location ->
                    put("p_location", buildJsonObject {
                        put("lat", location.latitude)
                        put("lng", location.longitude)
                        put("address", location.address)
                        put("city", location.city)
                        put("country", location.country)
                    })
                }
                details.businessCategory?.let { put("p_business_category", it) }
                details.description?.let { put("p_description", it) }
                details.website?.let { put("p_website", it) }
            }
            
            supabaseClient.postgrest.rpc(
                function = "update_business_details",
                parameters = params
            ).call<Boolean>()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating business details")
            Result.failure(e)
        }
    }
    
    override suspend fun updateNotificationPreferences(
        userId: String,
        preferences: NotificationPreferences
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_email_enabled", preferences.emailEnabled)
                put("p_sms_enabled", preferences.smsEnabled)
                put("p_push_enabled", preferences.pushEnabled)
                put("p_whatsapp_enabled", preferences.whatsappEnabled)
                put("p_events_config", buildJsonObject {
                    put("transaction_success", preferences.events.transactionSuccess)
                    put("transaction_failed", preferences.events.transactionFailed)
                    put("daily_summary", preferences.events.dailySummary)
                    put("weekly_report", preferences.events.weeklyReport)
                    put("security_alerts", preferences.events.securityAlerts)
                    put("system_updates", preferences.events.systemUpdates)
                })
                preferences.quietHours?.let { quietHours ->
                    put("p_quiet_hours", buildJsonObject {
                        put("start", quietHours.startTime)
                        put("end", quietHours.endTime)
                        put("enabled", quietHours.enabled)
                    })
                }
            }
            
            supabaseClient.postgrest.rpc(
                function = "update_notification_preferences",
                parameters = params
            ).call<Boolean>()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating notification preferences")
            Result.failure(e)
        }
    }
    
    override suspend fun updateTransactionLimits(
        userId: String,
        limits: TransactionLimits
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                limits.dailyLimit?.let { put("p_daily_limit", it.toString()) }
                limits.singleTransactionLimit?.let { put("p_single_transaction_limit", it.toString()) }
                limits.monthlyLimit?.let { put("p_monthly_limit", it.toString()) }
                put("p_minimum_amount", limits.minimumAmount.toString())
                limits.maximumAmount?.let { put("p_maximum_amount", it.toString()) }
                put("p_currency", limits.currency)
                limits.requireApprovalAbove?.let { put("p_require_approval_above", it.toString()) }
            }
            
            supabaseClient.postgrest.rpc(
                function = "update_transaction_limits",
                parameters = params
            ).call<Boolean>()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating transaction limits")
            Result.failure(e)
        }
    }
    
    override suspend fun updateFeatureFlags(
        userId: String,
        flags: FeatureFlags
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_nfc_enabled", flags.nfcEnabled)
                put("p_offline_mode", flags.offlineMode)
                put("p_auto_sync", flags.autoSync)
                put("p_biometric_required", flags.biometricRequired)
                put("p_receipts_enabled", flags.receiptsEnabled)
                put("p_multi_currency", flags.multiCurrency)
                put("p_advanced_analytics", flags.advancedAnalytics)
                put("p_api_access", flags.apiAccess)
            }
            
            supabaseClient.postgrest.rpc(
                function = "update_feature_flags",
                parameters = params
            ).call<Boolean>()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating feature flags")
            Result.failure(e)
        }
    }
    
    override suspend fun addPaymentProvider(
        userId: String,
        provider: PaymentProvider
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement via direct table insert
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding payment provider")
            Result.failure(e)
        }
    }
    
    override suspend fun removePaymentProvider(
        userId: String,
        providerId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement via direct table delete
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error removing payment provider")
            Result.failure(e)
        }
    }
    
    override suspend fun initializeSettings(
        userId: String,
        businessName: String,
        merchantCode: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_business_name", businessName)
                put("p_merchant_code", merchantCode)
            }
            
            val profileId = supabaseClient.postgrest.rpc(
                function = "initialize_merchant_settings",
                parameters = params
            ).call<String>()
            
            Result.success(profileId)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing merchant settings")
            Result.failure(e)
        }
    }
}
