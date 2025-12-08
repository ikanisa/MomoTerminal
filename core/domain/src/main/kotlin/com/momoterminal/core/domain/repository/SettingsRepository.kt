package com.momoterminal.core.domain.repository

import com.momoterminal.core.domain.model.settings.BusinessDetails
import com.momoterminal.core.domain.model.settings.FeatureFlags
import com.momoterminal.core.domain.model.settings.MerchantSettings
import com.momoterminal.core.domain.model.settings.MerchantStatus
import com.momoterminal.core.domain.model.settings.NotificationPreferences
import com.momoterminal.core.domain.model.settings.PaymentProvider
import com.momoterminal.core.domain.model.settings.TransactionLimits
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getMerchantSettings(userId: String): Result<MerchantSettings>
    
    fun observeMerchantSettings(userId: String): Flow<MerchantSettings>
    
    suspend fun updateProfile(
        userId: String,
        businessName: String? = null,
        status: MerchantStatus? = null
    ): Result<Unit>
    
    suspend fun updateBusinessDetails(
        userId: String,
        details: BusinessDetails
    ): Result<Unit>
    
    suspend fun updateNotificationPreferences(
        userId: String,
        preferences: NotificationPreferences
    ): Result<Unit>
    
    suspend fun updateTransactionLimits(
        userId: String,
        limits: TransactionLimits
    ): Result<Unit>
    
    suspend fun updateFeatureFlags(
        userId: String,
        flags: FeatureFlags
    ): Result<Unit>
    
    suspend fun addPaymentProvider(
        userId: String,
        provider: PaymentProvider
    ): Result<Unit>
    
    suspend fun removePaymentProvider(
        userId: String,
        providerId: String
    ): Result<Unit>
    
    suspend fun initializeSettings(
        userId: String,
        businessName: String,
        merchantCode: String
    ): Result<String>
}
