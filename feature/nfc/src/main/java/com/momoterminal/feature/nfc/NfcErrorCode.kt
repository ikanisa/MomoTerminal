package com.momoterminal.feature.nfc

/**
 * Error codes for NFC operations.
 * Used to provide specific error handling and user feedback.
 */
enum class NfcErrorCode(val code: Int, val description: String) {
    // Hardware errors
    NFC_NOT_SUPPORTED(1001, "NFC is not supported on this device"),
    NFC_DISABLED(1002, "NFC is disabled. Please enable it in settings"),
    NFC_HARDWARE_ERROR(1003, "NFC hardware error occurred"),
    
    // Communication errors
    CONNECTION_LOST(2001, "Connection lost during NFC transaction"),
    TIMEOUT(2002, "NFC transaction timed out"),
    APDU_ERROR(2003, "Error processing APDU command"),
    INVALID_RESPONSE(2004, "Invalid response received from reader"),
    
    // Payment errors
    INVALID_AMOUNT(3001, "Invalid payment amount"),
    INVALID_MERCHANT(3002, "Invalid merchant information"),
    PAYMENT_CANCELLED(3003, "Payment was cancelled"),
    PAYMENT_REJECTED(3004, "Payment was rejected"),
    
    // Configuration errors
    NOT_CONFIGURED(4001, "Merchant configuration is missing"),
    INVALID_CONFIG(4002, "Invalid configuration"),
    
    // Generic errors
    UNKNOWN_ERROR(9999, "An unknown error occurred");
    
    companion object {
        fun fromCode(code: Int): NfcErrorCode {
            return entries.find { it.code == code } ?: UNKNOWN_ERROR
        }
    }
}
