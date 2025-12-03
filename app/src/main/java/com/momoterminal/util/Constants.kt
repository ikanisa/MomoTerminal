package com.momoterminal.util

/**
 * Application-wide constants.
 */
object Constants {
    
    // Shared Preferences Keys
    const val PREFS_NAME = "momo_terminal_prefs"
    const val KEY_MERCHANT_CODE = "merchant_code"
    const val KEY_API_ENDPOINT = "api_endpoint"
    const val KEY_API_SECRET = "api_secret"
    const val KEY_MERCHANT_PHONE = "merchant_phone"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_IS_CONFIGURED = "is_configured"
    
    // Network
    const val DEFAULT_BASE_URL = "https://lhbowpbcpwoiparwnwgt.supabase.co/"
    const val DEFAULT_TIMEOUT_SECONDS = 30L
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L
    
    // Database
    const val DATABASE_NAME = "momoterminal_database"
    const val DATABASE_VERSION = 1
    
    // WorkManager
    const val SYNC_WORK_NAME = "momo_sync_work"
    const val PERIODIC_SYNC_WORK_NAME = "momo_periodic_sync"
    const val SYNC_INTERVAL_MINUTES = 15L
    
    // NFC
    const val NFC_AID = "D2760000850101"
    
    // Transaction Status
    const val STATUS_PENDING = "PENDING"
    const val STATUS_SENT = "SENT"
    const val STATUS_FAILED = "FAILED"
    
    // SMS Keywords for filtering (multi-country)
    val MOMO_KEYWORDS = listOf(
        "MoMo", "MobileMoney", "Mobile Money",
        "received", "sent", "payment", "credited", "transferred",
        // Providers
        "MTN", "Airtel", "Vodacom", "Vodafone", "Orange", "Tigo", "Wave", "M-Pesa",
        // Currencies
        "GHS", "RWF", "CDF", "XOF", "XAF", "TZS", "ZMW", "BIF"
    )
    
    // Provider Sender IDs (multi-country)
    val KNOWN_PROVIDER_SENDER_IDS = setOf(
        // MTN
        "MTN", "MobileMoney", "MTN MoMo", "MTN-MOMO", "MoMo",
        // Vodafone/Vodacom
        "VodaCash", "Vodafone", "VodafoneCash", "Vodacom", "M-Pesa", "MPESA",
        // Airtel
        "Airtel", "AirtelMoney", "Airtel Money",
        // Orange
        "Orange", "OrangeMoney", "Orange Money",
        // Tigo
        "Tigo", "TigoPesa", "Tigo Pesa",
        // Wave
        "Wave",
        // Others
        "AirtelTigo", "ATMoney", "AT-Money"
    )
}
