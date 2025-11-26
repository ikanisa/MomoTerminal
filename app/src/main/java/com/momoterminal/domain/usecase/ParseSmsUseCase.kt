package com.momoterminal.domain.usecase

import com.momoterminal.domain.model.Provider
import com.momoterminal.util.Result
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Use case for parsing SMS messages to extract payment information.
 */
class ParseSmsUseCase @Inject constructor() {
    
    /**
     * Parse an SMS message to extract payment details.
     * 
     * @param sender The SMS sender address
     * @param body The SMS message body
     * @return Result containing parsed SMS data or error
     */
    operator fun invoke(sender: String, body: String): Result<ParsedSmsData> {
        // Validate that this is a payment SMS
        if (!isPaymentSms(sender, body)) {
            return Result.Error(IllegalArgumentException("Not a valid payment SMS"))
        }
        
        // Detect provider
        val provider = Provider.fromSender(sender)
        
        // Parse amount
        val amount = parseAmount(body)
        
        // Parse transaction ID
        val transactionId = parseTransactionId(body)
        
        // Parse sender phone
        val senderPhone = parseSenderPhone(body)
        
        return Result.Success(
            ParsedSmsData(
                provider = provider,
                amount = amount,
                currency = "GHS",
                transactionId = transactionId,
                senderPhone = senderPhone,
                rawMessage = body,
                originalSender = sender
            )
        )
    }
    
    /**
     * Check if the SMS is a valid payment SMS.
     */
    private fun isPaymentSms(sender: String, body: String): Boolean {
        val knownProviders = setOf(
            "MTN", "MobileMoney", "MTN MoMo", "MTN-MOMO",
            "VodaCash", "Vodafone", "VodafoneCash",
            "AirtelTigo", "ATMoney", "AT-Money",
            "MoMo", "mPesa", "MPESA"
        )
        
        val isKnownSender = knownProviders.any { 
            sender.equals(it, ignoreCase = true) ||
            sender.startsWith(it, ignoreCase = true)
        }
        
        if (!isKnownSender) return false
        
        val hasAmount = body.contains(Regex("GH[SC]?\\s*\\d+", RegexOption.IGNORE_CASE)) ||
                       body.contains(Regex("\\d+\\.?\\d*\\s*GH[SC]?", RegexOption.IGNORE_CASE))
        
        val hasTransactionIndicator = body.contains("received", ignoreCase = true) ||
                                      body.contains("credited", ignoreCase = true) ||
                                      body.contains("transferred", ignoreCase = true) ||
                                      body.contains("payment", ignoreCase = true)
        
        return hasAmount && hasTransactionIndicator
    }
    
    /**
     * Parse amount from SMS body.
     */
    private fun parseAmount(body: String): Double {
        val amountPattern = Pattern.compile(
            "(GH[SC]?)\\s*(\\d+(?:\\.\\d{2})?)|" +
            "(\\d+(?:\\.\\d{2})?)\\s*(GH[SC]?)",
            Pattern.CASE_INSENSITIVE
        )
        
        val matcher = amountPattern.matcher(body)
        if (matcher.find()) {
            val amountStr = matcher.group(2) ?: matcher.group(3)
            return amountStr?.toDoubleOrNull() ?: 0.0
        }
        
        return 0.0
    }
    
    /**
     * Parse transaction ID from SMS body.
     */
    private fun parseTransactionId(body: String): String? {
        val patterns = listOf(
            Pattern.compile("(?:transaction\\s*(?:id|ref)?|txn\\s*(?:id)?|ref(?:erence)?)[:\\s#]*([A-Z0-9]{6,20})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b([A-Z]{2,4}[0-9]{6,12})\\b"),
            Pattern.compile("(?:id|ref)[:\\s]*([0-9]{12,16})\\b", Pattern.CASE_INSENSITIVE)
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        
        return null
    }
    
    /**
     * Parse sender phone number from SMS body.
     */
    private fun parseSenderPhone(body: String): String? {
        val phonePattern = Pattern.compile("\\b(0[235][0-9]{8})\\b|\\b(\\+233[0-9]{9})\\b")
        val matcher = phonePattern.matcher(body)
        
        if (matcher.find()) {
            return matcher.group(1) ?: matcher.group(2)
        }
        
        return null
    }
}

/**
 * Data class containing parsed SMS information.
 */
data class ParsedSmsData(
    val provider: Provider?,
    val amount: Double,
    val currency: String,
    val transactionId: String?,
    val senderPhone: String?,
    val rawMessage: String,
    val originalSender: String
)
