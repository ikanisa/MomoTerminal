package com.momoterminal.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.momoterminal.api.PaymentTransaction
import com.momoterminal.api.SyncService
import com.momoterminal.api.TransactionStatus
import java.util.regex.Pattern

/**
 * Legacy BroadcastReceiver for intercepting incoming SMS messages.
 * 
 * @deprecated Use [com.momoterminal.SmsReceiver] instead which uses Hilt DI
 * and supports webhook dispatching and offline-first database storage.
 * 
 * This legacy receiver is kept for reference of the parsing logic but should
 * not be registered in AndroidManifest.xml.
 */
@Deprecated(
    message = "Use com.momoterminal.SmsReceiver instead which uses Hilt DI and supports webhook dispatching",
    replaceWith = ReplaceWith("com.momoterminal.SmsReceiver")
)
class LegacySmsReceiver : BroadcastReceiver() {

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
     * Uses multiple validation layers to reduce false positives.
     */
    private fun isPaymentSms(sender: String, body: String): Boolean {
        // Known sender IDs for mobile money providers in Ghana
        // These are the official shortcodes/sender IDs
        val knownProviderSenderIds = setOf(
            "MTN", "MobileMoney", "MTN MoMo", "MTN-MOMO",
            "VodaCash", "Vodafone", "VodafoneCash",
            "AirtelTigo", "ATMoney", "AT-Money",
            "MoMo", "mPesa", "MPESA"
        )
        
        // Check if sender exactly matches known providers (case-insensitive)
        val isKnownSender = knownProviderSenderIds.any { 
            sender.equals(it, ignoreCase = true) ||
            sender.startsWith(it, ignoreCase = true)
        }
        
        // If sender is not from known provider, reject early
        // This prevents spoofing by arbitrary SMS sources
        if (!isKnownSender) {
            return false
        }
        
        // Additional validation: body must contain payment confirmation patterns
        // At minimum, must have both amount and transaction indicator
        val hasAmount = body.contains(Regex("GH[SC]?\\s*\\d+", RegexOption.IGNORE_CASE)) ||
                       body.contains(Regex("\\d+\\.?\\d*\\s*GH[SC]?", RegexOption.IGNORE_CASE))
        
        val hasTransactionIndicator = body.contains("received", ignoreCase = true) ||
                                      body.contains("credited", ignoreCase = true) ||
                                      body.contains("transferred", ignoreCase = true) ||
                                      body.contains("payment", ignoreCase = true)
        
        return hasAmount && hasTransactionIndicator
    }

    /**
     * Process and relay a payment SMS.
     */
    private fun processPaymentSms(context: Context, sender: String, body: String) {
        val parsedData = parsePaymentDetails(body)
        
        val transaction = PaymentTransaction(
            amountInPesewas = parsedData.amountInPesewas,
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
     * Returns amount in pesewas (smallest currency unit) for precision.
     */
    private fun parsePaymentDetails(body: String): ParsedPaymentData {
        var amountInPesewas = 0L
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
            val amountDouble = amountStr?.toDoubleOrNull() ?: 0.0
            amountInPesewas = (amountDouble * 100).toLong()
            currency = "GHS"
        }

        // Pattern for transaction ID (various formats)
        // More specific patterns to avoid matching phone numbers
        val txIdPatterns = listOf(
            // Explicitly labeled transaction IDs: "Transaction ID: ABC123456"
            Pattern.compile("(?:transaction\\s*(?:id|ref)?|txn\\s*(?:id)?|ref(?:erence)?)[:\\s#]*([A-Z0-9]{6,20})", Pattern.CASE_INSENSITIVE),
            // Format: 2-4 letter prefix followed by 6-12 digits (e.g., "MP123456789")
            Pattern.compile("\\b([A-Z]{2,4}[0-9]{6,12})\\b"),
            // Only match 12+ digit numbers that appear after "ID" or "Ref" context
            Pattern.compile("(?:id|ref)[:\\s]*([0-9]{12,16})\\b", Pattern.CASE_INSENSITIVE)
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
            amountInPesewas = amountInPesewas,
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
        private const val TAG = "LegacySmsReceiver"
        
        const val BROADCAST_PAYMENT_RECEIVED = "com.momoterminal.broadcast.PAYMENT_RECEIVED"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_CURRENCY = "extra_currency"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_RAW_MESSAGE = "extra_raw_message"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
}
