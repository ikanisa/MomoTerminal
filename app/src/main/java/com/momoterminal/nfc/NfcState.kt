package com.momoterminal.nfc

/**
 * Sealed class representing all possible NFC states.
 * Used for proper state management and UI updates.
 */
sealed class NfcState {
    /**
     * NFC is disabled on the device.
     */
    data object Disabled : NfcState()
    
    /**
     * NFC is not supported on this device.
     */
    data object NotSupported : NfcState()
    
    /**
     * NFC is ready and waiting for activation.
     */
    data object Ready : NfcState()
    
    /**
     * NFC is being activated with payment data.
     */
    data object Activating : NfcState()
    
    /**
     * NFC is active and broadcasting payment data.
     * @property paymentData The payment data being broadcast
     */
    data class Active(val paymentData: NfcPaymentData) : NfcState()
    
    /**
     * NFC transaction is being processed.
     * @property progress Progress from 0.0 to 1.0
     */
    data class Processing(val progress: Float) : NfcState()
    
    /**
     * NFC transaction completed successfully.
     * @property transactionId Unique transaction identifier
     */
    data class Success(val transactionId: String) : NfcState()
    
    /**
     * NFC operation encountered an error.
     * @property message Human-readable error message
     * @property code Error code for programmatic handling
     */
    data class Error(val message: String, val code: NfcErrorCode) : NfcState()
    
    /**
     * NFC operation timed out.
     */
    data object Timeout : NfcState()
    
    /**
     * Check if NFC is in an active broadcasting state.
     * Includes Activating state since UI may need to reflect this.
     */
    fun isActive(): Boolean = this is Activating || this is Active || this is Processing
    
    /**
     * Check if NFC is in a terminal state (success, error, timeout).
     */
    fun isTerminal(): Boolean = this is Success || this is Error || this is Timeout
    
    /**
     * Get display message for the current state.
     */
    fun getDisplayMessage(): String {
        return when (this) {
            is Disabled -> "NFC is disabled. Please enable it in settings."
            is NotSupported -> "NFC is not supported on this device."
            is Ready -> "Ready. Enter an amount to start."
            is Activating -> "Activating NFC..."
            is Active -> "Ready for tap. Hold device near reader."
            is Processing -> "Processing payment... ${(progress * 100).toInt()}%"
            is Success -> "Payment successful! Transaction: $transactionId"
            is Error -> message
            is Timeout -> "Transaction timed out. Please try again."
        }
    }
}
