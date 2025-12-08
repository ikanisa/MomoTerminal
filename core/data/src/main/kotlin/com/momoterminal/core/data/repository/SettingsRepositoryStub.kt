package com.momoterminal.core.data.repository

import com.momoterminal.core.domain.model.settings.*
import com.momoterminal.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of SettingsRepository for initial build.
 * TODO: Replace with full Supabase RPC implementation once backend is ready.
 * 
 * This stub returns mock data to allow the app to compile and run.
 * Settings are not persisted and will reset on app restart.
 */
@Singleton
class SettingsRepositoryStub @Inject constructor() : SettingsRepository {
    
    private var cachedSettings: MerchantSettings? = null
    
    override suspend fun getMerchantSettings(userId: String): Result<MerchantSettings> {
        return try {
            val settings = cachedSettings ?: createDefaultSettings(userId)
            cachedSettings = settings
            Timber.d("Returning stub merchant settings for user: $userId")
            Result.success(settings)
        } catch (e: Exception) {
            Timber.e(e, "Error in stub getMerchantSettings")
            Result.failure(e)
        }
    }
    
    override fun observeMerchantSettings(userId: String): Flow<MerchantSettings> {
        val settings = cachedSettings ?: createDefaultSettings(userId)
        return flowOf(settings)
    }
    
    override suspend fun updateProfile(
        userId: String,
        businessName: String?,
        status: MerchantStatus?
    ): Result<Unit> {
        Timber.d("Stub: updateProfile called (not persisted)")
        cachedSettings = cachedSettings?.copy(
            profile = cachedSettings!!.profile.copy(
                businessName = businessName ?: cachedSettings!!.profile.businessName,
                status = status ?: cachedSettings!!.profile.status
            )
        )
        return Result.success(Unit)
    }
    
    override suspend fun updateBusinessDetails(
        userId: String,
        details: BusinessDetails
    ): Result<Unit> {
        Timber.d("Stub: updateBusinessDetails called (not persisted)")
        cachedSettings = cachedSettings?.copy(businessDetails = details)
        return Result.success(Unit)
    }
    
    override suspend fun updateNotificationPreferences(
        userId: String,
        preferences: NotificationPreferences
    ): Result<Unit> {
        Timber.d("Stub: updateNotificationPreferences called (not persisted)")
        cachedSettings = cachedSettings?.copy(notificationPrefs = preferences)
        return Result.success(Unit)
    }
    
    override suspend fun updateTransactionLimits(
        userId: String,
        limits: TransactionLimits
    ): Result<Unit> {
        Timber.d("Stub: updateTransactionLimits called (not persisted)")
        cachedSettings = cachedSettings?.copy(transactionLimits = limits)
        return Result.success(Unit)
    }
    
    override suspend fun updateFeatureFlags(
        userId: String,
        flags: FeatureFlags
    ): Result<Unit> {
        Timber.d("Stub: updateFeatureFlags called (not persisted)")
        cachedSettings = cachedSettings?.copy(featureFlags = flags)
        return Result.success(Unit)
    }
    
    override suspend fun addPaymentProvider(
        userId: String,
        provider: PaymentProvider
    ): Result<Unit> {
        Timber.d("Stub: addPaymentProvider called (not persisted)")
        return Result.success(Unit)
    }
    
    override suspend fun removePaymentProvider(
        userId: String,
        providerId: String
    ): Result<Unit> {
        Timber.d("Stub: removePaymentProvider called (not persisted)")
        return Result.success(Unit)
    }
    
    override suspend fun initializeSettings(
        userId: String,
        businessName: String,
        merchantCode: String
    ): Result<String> {
        Timber.d("Stub: initializeSettings called")
        cachedSettings = createDefaultSettings(userId, businessName, merchantCode)
        return Result.success(userId)
    }
    
    private fun createDefaultSettings(
        userId: String,
        businessName: String = "My Business",
        merchantCode: String = "MERCHANT001"
    ): MerchantSettings {
        val now = java.time.Instant.now()
        return MerchantSettings(
            profile = MerchantProfile(
                id = userId,
                userId = userId,
                businessName = businessName,
                merchantCode = merchantCode,
                status = MerchantStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            ),
            businessDetails = BusinessDetails(
                businessType = BusinessType.SOLE_PROPRIETOR,
                taxId = null,
                registrationNumber = null,
                location = null,
                businessCategory = "General",
                description = null,
                website = null
            ),
            contactInfo = ContactInfo(),
            notificationPrefs = NotificationPreferences(
                emailEnabled = true,
                smsEnabled = true,
                pushEnabled = true,
                whatsappEnabled = false,
                events = NotificationEvents(
                    transactionSuccess = true,
                    transactionFailed = true,
                    dailySummary = false,
                    weeklyReport = false,
                    securityAlerts = true,
                    systemUpdates = true
                ),
                quietHours = null
            ),
            transactionLimits = TransactionLimits(
                dailyLimit = java.math.BigDecimal("10000.00"),
                singleTransactionLimit = java.math.BigDecimal("5000.00"),
                monthlyLimit = java.math.BigDecimal("200000.00"),
                minimumAmount = java.math.BigDecimal("1.00"),
                maximumAmount = java.math.BigDecimal("5000.00"),
                currency = "GHS",
                requireApprovalAbove = null
            ),
            featureFlags = FeatureFlags(
                nfcEnabled = true,
                offlineMode = true,
                autoSync = true,
                biometricRequired = false,
                receiptsEnabled = true,
                multiCurrency = false,
                advancedAnalytics = false,
                apiAccess = false
            ),
            paymentProviders = emptyList()
        )
    }
}
