package com.momoterminal.feature.payment.nfc

import androidx.lifecycle.MutableLiveData

/**
 * Simple singleton to share state during NFC payment processing.
 * Used by NfcHceService for logging and status updates.
 * Syncs with NfcManager in feature:nfc module.
 */
object PaymentState {
    var currentAmount: String? = null
    var currentPaymentUri: String? = null
    val statusUpdate = MutableLiveData<String>()
    
    /**
     * Set payment data for NFC broadcasting.
     * Called by NfcManager when payment is activated.
     */
    fun setPaymentData(amount: Long, merchantPhone: String, currency: String = "RWF") {
        currentAmount = amount.toString()
        // Create USSD URI for MTN Rwanda
        currentPaymentUri = "tel:*182*1*1*$merchantPhone*$amount#"
        appendLog("Payment ready: $amount $currency")
    }
    
    fun appendLog(message: String) {
        // Simple logging - can be expanded to store history
        statusUpdate.postValue(message)
    }
    
    fun reset() {
        currentAmount = null
        currentPaymentUri = null
    }
}
