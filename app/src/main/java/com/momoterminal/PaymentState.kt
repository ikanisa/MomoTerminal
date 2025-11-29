package com.momoterminal

import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton object to manage shared payment state across the application.
 * Handles payment URIs, webhook configuration, and logging.
 * 
 * Note: For monetary precision, amounts should be stored in pesewas (smallest currency unit).
 * 1 GHS = 100 pesewas.
 */
object PaymentState {
    
    // Current payment URI for NFC broadcasting
    var currentPaymentUri: String? = null
    
    // Current payment amount for NFC broadcasting (in pesewas)
    var currentAmountInPesewas: Long? = null
    
    /**
     * Get the current amount as a formatted string for NFC broadcasting.
     * Returns the amount in GHS (e.g., "10.50") or null if not set.
     */
    val currentAmount: String?
        get() = currentAmountInPesewas?.let { "%.2f".format(it / 100.0) }
    
    // LiveData for SMS log updates
    val smsLog: MutableLiveData<String> = MutableLiveData("")
    
    // LiveData for status updates
    val statusUpdate: MutableLiveData<String> = MutableLiveData("")
    
    /**
     * Generate a USSD string for Mobile Money payment.
     * @param merchant The merchant phone number
     * @param amountInPesewas The payment amount in pesewas
     * @return USSD dial string
     */
    fun generateMomoUssd(merchant: String, amountInPesewas: Long): String {
        val formattedAmount = "%.2f".format(amountInPesewas / 100.0)
        return "tel:*182*1*1*${merchant}*${formattedAmount}#"
    }
    
    /**
     * Generate a payment URI for NFC transmission.
     * @param merchant The merchant phone number
     * @param amountInPesewas The payment amount in pesewas
     * @return Payment URI string
     */
    fun generatePaymentUri(merchant: String, amountInPesewas: Long): String {
        val formattedAmount = "%.2f".format(amountInPesewas / 100.0)
        return "momo://pay?to=$merchant&amount=$formattedAmount&currency=GHS"
    }
    
    /**
     * Append a timestamped message to the SMS log.
     * @param message The message to append
     */
    fun appendLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentLog = smsLog.value ?: ""
        val newLog = "[$timestamp] $message\n$currentLog"
        smsLog.postValue(newLog)
    }
    
    /**
     * Clear the current payment state and reset URI.
     */
    fun clearPayment() {
        currentPaymentUri = null
        currentAmountInPesewas = null
        statusUpdate.postValue("Payment cancelled")
    }
}
