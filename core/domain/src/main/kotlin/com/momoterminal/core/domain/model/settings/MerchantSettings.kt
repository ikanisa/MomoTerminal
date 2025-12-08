package com.momoterminal.core.domain.model.settings

import java.math.BigDecimal
import java.time.Instant

/**
 * Merchant Profile - Core identity and status
 */
data class MerchantProfile(
    val id: String,
    val userId: String,
    val businessName: String,
    val merchantCode: String,
    val status: MerchantStatus = MerchantStatus.ACTIVE,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class MerchantStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}

/**
 * Business Details - Registration and categorization
 */
data class BusinessDetails(
    val businessType: BusinessType? = null,
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val location: Location? = null,
    val businessCategory: String? = null,
    val description: String? = null,
    val website: String? = null
)

enum class BusinessType {
    SOLE_PROPRIETOR,
    PARTNERSHIP,
    CORPORATION,
    LLC,
    COOPERATIVE,
    NGO,
    OTHER
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val country: String
)

/**
 * Contact Information
 */
data class ContactInfo(
    val email: String? = null,
    val phone: String? = null,
    val whatsapp: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null
)

/**
 * Notification Preferences
 */
data class NotificationPreferences(
    val emailEnabled: Boolean = true,
    val smsEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val whatsappEnabled: Boolean = false,
    val events: NotificationEvents = NotificationEvents(),
    val quietHours: QuietHours? = null
)

data class NotificationEvents(
    val transactionSuccess: Boolean = true,
    val transactionFailed: Boolean = true,
    val dailySummary: Boolean = true,
    val weeklyReport: Boolean = false,
    val securityAlerts: Boolean = true,
    val systemUpdates: Boolean = false
)

data class QuietHours(
    val startTime: String,
    val endTime: String,
    val enabled: Boolean = true
)

/**
 * Transaction Limits
 */
data class TransactionLimits(
    val dailyLimit: BigDecimal? = null,
    val singleTransactionLimit: BigDecimal? = null,
    val monthlyLimit: BigDecimal? = null,
    val minimumAmount: BigDecimal = BigDecimal("100.00"),
    val maximumAmount: BigDecimal? = null,
    val currency: String = "XAF",
    val requireApprovalAbove: BigDecimal? = null
)

/**
 * Feature Flags
 */
data class FeatureFlags(
    val nfcEnabled: Boolean = false,
    val offlineMode: Boolean = true,
    val autoSync: Boolean = true,
    val biometricRequired: Boolean = false,
    val receiptsEnabled: Boolean = true,
    val multiCurrency: Boolean = false,
    val advancedAnalytics: Boolean = false,
    val apiAccess: Boolean = false
)

/**
 * Payment Provider Configuration
 */
data class PaymentProvider(
    val id: String,
    val providerName: String,
    val isPreferred: Boolean = false,
    val isEnabled: Boolean = true,
    val settings: Map<String, Any> = emptyMap()
)

/**
 * Complete Merchant Settings - Aggregate root
 */
data class MerchantSettings(
    val profile: MerchantProfile,
    val businessDetails: BusinessDetails = BusinessDetails(),
    val contactInfo: ContactInfo = ContactInfo(),
    val notificationPrefs: NotificationPreferences = NotificationPreferences(),
    val transactionLimits: TransactionLimits = TransactionLimits(),
    val featureFlags: FeatureFlags = FeatureFlags(),
    val paymentProviders: List<PaymentProvider> = emptyList()
)
