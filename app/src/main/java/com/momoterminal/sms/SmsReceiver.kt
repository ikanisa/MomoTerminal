package com.momoterminal.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.momoterminal.api.PaymentTransaction
import com.momoterminal.api.SyncService
import com.momoterminal.api.TransactionStatus
import java.util.UUID
import java.util.regex.Pattern

/**
 * BroadcastReceiver for intercepting incoming SMS messages.
 * 
 * This receiver listens for incoming SMS, identifies payment confirmations
 * from mobile money providers, and relays them to the PWA database for
 * real-time financial tracking.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        for (smsMessage in messages) {
            val sender = smsMessage.originatingAddress ?: continue
            val body = smsMessage.messageBody ?: continue
            
            Log.d(TAG, "SMS received from: $sender")
            
            // Check if this is a mobile money message
            if (isPaymentSms(sender, body)) {
                Log.d(TAG, "Payment SMS detected")
                processPaymentSms(context, sender, body)
            }
        }
    }

    /**
     * Check if the SMS is from a mobile money provider.
     */
    private fun isPaymentSms(sender: String, body: String): Boolean {
        // Check sender ID patterns for common mobile money providers
        val mobileMoneyProviders = listOf(
            "MTN", "MobileMoney", "MOMO", "VodaCash", "Vodafone",
            "AirtelTigo", "ATMoney", "MoMo", "mPesa", "MPESA"
        )
        
        // Check if sender matches known providers
        val isMomoSender = mobileMoneyProviders.any { 
            sender.contains(it, ignoreCase = true) 
        }
        
        // Check if body contains payment-related keywords
        val paymentKeywords = listOf(
            "received", "payment", "transferred", "credited",
            "GHS", "GHC", "amount", "from", "transaction"
        )
        
        val hasPaymentKeyword = paymentKeywords.any { 
            body.contains(it, ignoreCase = true) 
        }
        
        return isMomoSender || hasPaymentKeyword
    }

    /**
     * Process and relay a payment SMS.
     */
    private fun processPaymentSms(context: Context, sender: String, body: String) {
        val parsedData = parsePaymentDetails(body)
        
        val transaction = PaymentTransaction(
            amount = parsedData.amount,
            currency = parsedData.currency,
            senderNumber = parsedData.senderPhone ?: sender,
            transactionId = parsedData.transactionId ?: generateTransactionId(),
            rawMessage = body,
            status = TransactionStatus.PENDING,
            merchantCode = getMerchantCode(context)
        )

        // Broadcast the transaction locally
        broadcastTransaction(context, transaction)

        // Start sync service to push to PWA database
        startSyncService(context, transaction)
    }

    /**
     * Parse payment details from SMS body.
     */
    private fun parsePaymentDetails(body: String): ParsedPaymentData {
        var amount = 0.0
        var currency = "GHS"
        var transactionId: String? = null
        var senderPhone: String? = null

        // Pattern for amount (e.g., "GHS 50.00" or "GHC50" or "50.00 GHS")
        val amountPattern = Pattern.compile(
            "(GH[SC]?)\\s*(\\d+(?:\\.\\d{2})?)|" +
            "(\\d+(?:\\.\\d{2})?)\\s*(GH[SC]?)",
            Pattern.CASE_INSENSITIVE
        )
        val amountMatcher = amountPattern.matcher(body)
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(2) ?: amountMatcher.group(3)
            amount = amountStr?.toDoubleOrNull() ?: 0.0
            currency = "GHS"
        }

        // Pattern for transaction ID (various formats)
        val txIdPatterns = listOf(
            Pattern.compile("(?:transaction|txn|ref|id)[:\\s#]*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([A-Z]{2,4}[0-9]{6,12})\\b"),
            Pattern.compile("\\b([0-9]{10,14})\\b")
        )
        for (pattern in txIdPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                transactionId = matcher.group(1)
                break
            }
        }

        // Pattern for phone number
        val phonePattern = Pattern.compile("\\b(0[235][0-9]{8})\\b|\\b(\\+233[0-9]{9})\\b")
        val phoneMatcher = phonePattern.matcher(body)
        if (phoneMatcher.find()) {
            senderPhone = phoneMatcher.group(1) ?: phoneMatcher.group(2)
        }

        return ParsedPaymentData(
            amount = amount,
            currency = currency,
            transactionId = transactionId,
            senderPhone = senderPhone
        )
    }

    /**
     * Generate a unique transaction ID.
     */
    private fun generateTransactionId(): String {
        return "MT${System.currentTimeMillis()}"
    }

    /**
     * Get merchant code from shared preferences.
     */
    private fun getMerchantCode(context: Context): String? {
        return context.getSharedPreferences("momo_terminal_prefs", Context.MODE_PRIVATE)
            .getString("merchant_code", null)
    }

    /**
     * Broadcast the transaction for local UI updates.
     */
    private fun broadcastTransaction(context: Context, transaction: PaymentTransaction) {
        val intent = Intent(BROADCAST_PAYMENT_RECEIVED).apply {
            putExtra(EXTRA_AMOUNT, transaction.amount)
            putExtra(EXTRA_CURRENCY, transaction.currency)
            putExtra(EXTRA_SENDER, transaction.senderNumber)
            putExtra(EXTRA_TRANSACTION_ID, transaction.transactionId)
            putExtra(EXTRA_RAW_MESSAGE, transaction.rawMessage)
            putExtra(EXTRA_TIMESTAMP, transaction.timestamp)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Start the sync service to push transaction to PWA.
     */
    private fun startSyncService(context: Context, transaction: PaymentTransaction) {
        val intent = Intent(context, SyncService::class.java).apply {
            action = SyncService.ACTION_SYNC_TRANSACTION
            putExtra(SyncService.EXTRA_TRANSACTION, transaction)
        }
        context.startService(intent)
    }

    /**
     * Data class for parsed payment information.
     */
    private data class ParsedPaymentData(
        val amount: Double,
        val currency: String,
        val transactionId: String?,
        val senderPhone: String?
    )

    companion object {
        private const val TAG = "SmsReceiver"
        
        const val BROADCAST_PAYMENT_RECEIVED = "com.momoterminal.broadcast.PAYMENT_RECEIVED"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_CURRENCY = "extra_currency"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_RAW_MESSAGE = "extra_raw_message"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
}
