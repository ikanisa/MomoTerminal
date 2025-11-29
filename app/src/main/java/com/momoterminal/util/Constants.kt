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
    const val DEFAULT_BASE_URL = "https://api.momoterminal.com/"
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
    
    // Currency
    const val DEFAULT_CURRENCY = "GHS"
    
    // Transaction Status
    const val STATUS_PENDING = "PENDING"
    const val STATUS_SENT = "SENT"
    const val STATUS_FAILED = "FAILED"
    
    // SMS Keywords for filtering (Ghana-focused)
    val MOMO_KEYWORDS = listOf(
        "MOMO", "MobileMoney", "MTN", "GHS",
        "received", "sent", "payment", "credited",
        "transferred", "VodaCash", "AirtelTigo"
    )
    
    // Provider Sender IDs
    val KNOWN_PROVIDER_SENDER_IDS = setOf(
        "MTN", "MobileMoney", "MTN MoMo", "MTN-MOMO",
        "VodaCash", "Vodafone", "VodafoneCash",
        "AirtelTigo", "ATMoney", "AT-Money",
        "MoMo", "mPesa", "MPESA"
    )
}
