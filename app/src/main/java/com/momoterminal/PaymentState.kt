package com.momoterminal

import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton object to manage shared payment state across the application.
 * Handles payment URIs, webhook configuration, and logging.
 */
object PaymentState {
    
    // Current payment URI for NFC broadcasting
    var currentPaymentUri: String? = null
    
    // Webhook URL for SMS relay. 
    // Configure this value before using the SMS relay feature.
    // For testing, use https://webhook.site to generate a unique URL.
    var webhookUrl: String = ""
    
    // LiveData for SMS log updates
    val smsLog: MutableLiveData<String> = MutableLiveData("")
    
    // LiveData for status updates
    val statusUpdate: MutableLiveData<String> = MutableLiveData("")
    
    /**
     * Generate a USSD string for Mobile Money payment.
     * @param merchant The merchant phone number
     * @param amount The payment amount
     * @return USSD dial string
     */
    fun generateMomoUssd(merchant: String, amount: String): String {
        return "tel:*182*1*1*${merchant}*${amount}#"
    }
    
    /**
     * Generate a payment URI for NFC transmission.
     * @param merchant The merchant phone number
     * @param amount The payment amount
     * @return Payment URI string
     */
    fun generatePaymentUri(merchant: String, amount: String): String {
        return "momo://pay?to=$merchant&amount=$amount&currency=RWF"
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
        statusUpdate.postValue("Payment cancelled")
    }
}
