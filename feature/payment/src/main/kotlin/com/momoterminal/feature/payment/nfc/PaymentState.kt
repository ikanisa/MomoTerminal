package com.momoterminal.feature.payment.nfc

import androidx.lifecycle.MutableLiveData

/**
 * Simple singleton to share state during NFC payment processing.
 * Used by NfcHceService for logging and status updates.
 */
object PaymentState {
    var currentAmount: String? = null
    var currentPaymentUri: String? = null
    val statusUpdate = MutableLiveData<String>()
    
    fun appendLog(message: String) {
        // Simple logging - can be expanded to store history
        statusUpdate.postValue(message)
    }
    
    fun reset() {
        currentAmount = null
        currentPaymentUri = null
    }
}
