package com.momoterminal.feature.nfc

/**
 * Represents the various states of NFC operations.
 */
sealed class NfcState {
    /** NFC is ready and waiting for activation */
    data object Ready : NfcState()
    
    /** NFC is being activated */
    data object Activating : NfcState()
    
    /** NFC is active and broadcasting payment data */
    data class Active(val paymentData: NfcPaymentData) : NfcState()
    
    /** NFC transaction is being processed */
    data class Processing(val progress: Float = 0f) : NfcState()
    
    /** NFC transaction completed successfully */
    data class Success(val transactionId: String) : NfcState()
    
    /** NFC transaction failed with an error */
    data class Error(val message: String, val errorCode: NfcErrorCode) : NfcState()
    
    /** NFC transaction timed out */
    data object Timeout : NfcState()
    
    /** NFC is not supported on this device */
    data object NotSupported : NfcState()
    
    /** NFC is disabled in device settings */
    data object Disabled : NfcState()
    
    /** Get user-friendly display message for current state */
    fun getDisplayMessage(): String = when (this) {
        is Ready -> "Ready to accept payment"
        is Activating -> "Activating NFC..."
        is Active -> "Tap customer's phone to pay"
        is Processing -> "Processing payment..."
        is Success -> "Payment successful! ID: $transactionId"
        is Error -> message
        is Timeout -> "Transaction timed out. Please try again."
        is NotSupported -> "NFC is not supported on this device"
        is Disabled -> "Please enable NFC in device settings"
    }
    
    /** Check if NFC is in an active/working state */
    fun isWorking(): Boolean = this is Active || this is Activating || this is Processing
}
