package com.momoterminal.domain.model

/**
 * Domain model representing a merchant.
 */
data class Merchant(
    val merchantCode: String,
    val businessName: String? = null,
    val phoneNumber: String,
    val deviceId: String? = null,
    val apiEndpoint: String? = null,
    val isConfigured: Boolean = false
)
