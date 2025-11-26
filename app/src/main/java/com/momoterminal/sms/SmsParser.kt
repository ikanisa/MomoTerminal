package com.momoterminal.sms

import android.util.Log
import java.util.regex.Pattern

/**
 * Parser for Mobile Money SMS messages.
 * Supports MTN, Vodafone, and AirtelTigo providers in Ghana.
 */
object SmsParser {
    private const val TAG = "SmsParser"
    
    /**
     * Parsed transaction data from SMS.
     */
    data class ParsedTransaction(
        val provider: String,
        val transactionType: TransactionType,
        val amount: Double,
        val currency: String,
        val senderOrRecipient: String?,
        val transactionId: String?,
        val balance: Double?,
        val timestamp: Long = System.currentTimeMillis(),
        val rawMessage: String
    )
    
    /**
     * Transaction types that can be parsed from SMS.
     */
    enum class TransactionType {
        RECEIVED,
        SENT,
        PAYMENT,
        WITHDRAWAL,
        DEPOSIT,
        AIRTIME,
        UNKNOWN
    }
    
    // MTN MoMo patterns
    private val MTN_RECEIVED_PATTERN = Pattern.compile(
        "(?:You have received|Received)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+from\\s+(.+?)(?:\\.|\\s+Trans)",
        Pattern.CASE_INSENSITIVE
    )
    private val MTN_SENT_PATTERN = Pattern.compile(
        "(?:You have sent|Sent|Transfer of)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+to\\s+(.+?)(?:\\.|\\s+Trans)",
        Pattern.CASE_INSENSITIVE
    )
    private val MTN_PAYMENT_PATTERN = Pattern.compile(
        "(?:Payment of|Paid)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+to\\s+(.+?)(?:\\.|\\s+Trans)",
        Pattern.CASE_INSENSITIVE
    )
    private val MTN_BALANCE_PATTERN = Pattern.compile(
        "(?:balance|bal)\\s*(?:is|:)?\\s*GH[SC]?\\s*([\\d,]+\\.?\\d*)",
        Pattern.CASE_INSENSITIVE
    )
    private val MTN_TRANS_ID_PATTERN = Pattern.compile(
        "(?:Trans(?:action)?\\s*(?:ID|Id|id)?\\s*(?:is|:)?|ID:?)\\s*([A-Z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    
    // Vodafone Cash patterns
    private val VODAFONE_RECEIVED_PATTERN = Pattern.compile(
        "(?:Received|You have received)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+from\\s+(.+?)(?:\\.|\\s+Ref)",
        Pattern.CASE_INSENSITIVE
    )
    private val VODAFONE_SENT_PATTERN = Pattern.compile(
        "(?:Sent|You have sent|Transferred)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+to\\s+(.+?)(?:\\.|\\s+Ref)",
        Pattern.CASE_INSENSITIVE
    )
    private val VODAFONE_BALANCE_PATTERN = Pattern.compile(
        "(?:balance|Available)\\s*(?:is|:)?\\s*GH[SC]?\\s*([\\d,]+\\.?\\d*)",
        Pattern.CASE_INSENSITIVE
    )
    private val VODAFONE_REF_PATTERN = Pattern.compile(
        "(?:Ref(?:erence)?\\s*(?:No)?\\s*(?:is|:)?|Ref:?)\\s*([A-Z0-9]+)",
        Pattern.CASE_INSENSITIVE
    )
    
    // AirtelTigo Money patterns
    private val AIRTELTIGO_RECEIVED_PATTERN = Pattern.compile(
        "(?:Credited|Received)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+from\\s+(.+?)(?:\\.|\\s+Trans)",
        Pattern.CASE_INSENSITIVE
    )
    private val AIRTELTIGO_SENT_PATTERN = Pattern.compile(
        "(?:Debited|Sent)\\s+GH[SC]?\\s*([\\d,]+\\.?\\d*)\\s+to\\s+(.+?)(?:\\.|\\s+Trans)",
        Pattern.CASE_INSENSITIVE
    )
    private val AIRTELTIGO_BALANCE_PATTERN = Pattern.compile(
        "(?:balance|Bal)\\s*(?:is|:)?\\s*GH[SC]?\\s*([\\d,]+\\.?\\d*)",
        Pattern.CASE_INSENSITIVE
    )
    
    // Generic amount pattern
    private val AMOUNT_PATTERN = Pattern.compile(
        "GH[SC]?\\s*([\\d,]+\\.?\\d*)",
        Pattern.CASE_INSENSITIVE
    )
    
    // Provider detection keywords
    private val MTN_KEYWORDS = listOf("MTN", "MOMO", "MoMo", "Mobile Money")
    private val VODAFONE_KEYWORDS = listOf("Vodafone", "VCash", "Vodafone Cash")
    private val AIRTELTIGO_KEYWORDS = listOf("AirtelTigo", "AT Money", "Airtel", "Tigo")
    
    /**
     * Parse an SMS message and extract transaction data.
     * @param sender The SMS sender address
     * @param body The SMS body text
     * @return ParsedTransaction if successfully parsed, null otherwise
     */
    fun parseSms(sender: String, body: String): ParsedTransaction? {
        val provider = detectProvider(sender, body)
        Log.d(TAG, "Detected provider: $provider")
        
        return when (provider) {
            "MTN" -> parseMtnSms(body)
            "VODAFONE" -> parseVodafoneSms(body)
            "AIRTELTIGO" -> parseAirtelTigoSms(body)
            else -> parseGenericSms(body)
        }
    }
    
    /**
     * Detect the provider from sender or message content.
     */
    private fun detectProvider(sender: String, body: String): String? {
        val combined = "$sender $body"
        
        return when {
            MTN_KEYWORDS.any { combined.contains(it, ignoreCase = true) } -> "MTN"
            VODAFONE_KEYWORDS.any { combined.contains(it, ignoreCase = true) } -> "VODAFONE"
            AIRTELTIGO_KEYWORDS.any { combined.contains(it, ignoreCase = true) } -> "AIRTELTIGO"
            else -> null
        }
    }
    
    /**
     * Parse MTN MoMo SMS.
     */
    private fun parseMtnSms(body: String): ParsedTransaction? {
        val (type, amount, party) = parseTransactionDetails(
            body,
            MTN_RECEIVED_PATTERN,
            MTN_SENT_PATTERN,
            MTN_PAYMENT_PATTERN
        ) ?: return null
        
        val balance = extractBalance(body, MTN_BALANCE_PATTERN)
        val transactionId = extractTransactionId(body, MTN_TRANS_ID_PATTERN)
        
        return ParsedTransaction(
            provider = "MTN",
            transactionType = type,
            amount = amount,
            currency = "GHS",
            senderOrRecipient = party,
            transactionId = transactionId,
            balance = balance,
            rawMessage = body
        )
    }
    
    /**
     * Parse Vodafone Cash SMS.
     */
    private fun parseVodafoneSms(body: String): ParsedTransaction? {
        val (type, amount, party) = parseTransactionDetails(
            body,
            VODAFONE_RECEIVED_PATTERN,
            VODAFONE_SENT_PATTERN,
            null
        ) ?: return null
        
        val balance = extractBalance(body, VODAFONE_BALANCE_PATTERN)
        val transactionId = extractTransactionId(body, VODAFONE_REF_PATTERN)
        
        return ParsedTransaction(
            provider = "VODAFONE",
            transactionType = type,
            amount = amount,
            currency = "GHS",
            senderOrRecipient = party,
            transactionId = transactionId,
            balance = balance,
            rawMessage = body
        )
    }
    
    /**
     * Parse AirtelTigo Money SMS.
     */
    private fun parseAirtelTigoSms(body: String): ParsedTransaction? {
        val (type, amount, party) = parseTransactionDetails(
            body,
            AIRTELTIGO_RECEIVED_PATTERN,
            AIRTELTIGO_SENT_PATTERN,
            null
        ) ?: return null
        
        val balance = extractBalance(body, AIRTELTIGO_BALANCE_PATTERN)
        
        return ParsedTransaction(
            provider = "AIRTELTIGO",
            transactionType = type,
            amount = amount,
            currency = "GHS",
            senderOrRecipient = party,
            transactionId = null,
            balance = balance,
            rawMessage = body
        )
    }
    
    /**
     * Parse generic Mobile Money SMS.
     */
    private fun parseGenericSms(body: String): ParsedTransaction? {
        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        if (!amountMatcher.find()) return null
        
        val amount = parseAmount(amountMatcher.group(1) ?: return null)
        val type = when {
            body.contains("received", ignoreCase = true) -> TransactionType.RECEIVED
            body.contains("sent", ignoreCase = true) -> TransactionType.SENT
            body.contains("payment", ignoreCase = true) -> TransactionType.PAYMENT
            else -> TransactionType.UNKNOWN
        }
        
        return ParsedTransaction(
            provider = "UNKNOWN",
            transactionType = type,
            amount = amount,
            currency = "GHS",
            senderOrRecipient = null,
            transactionId = null,
            balance = null,
            rawMessage = body
        )
    }
    
    /**
     * Parse transaction details from patterns.
     */
    private fun parseTransactionDetails(
        body: String,
        receivedPattern: Pattern,
        sentPattern: Pattern,
        paymentPattern: Pattern?
    ): Triple<TransactionType, Double, String?>? {
        var matcher = receivedPattern.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1) ?: return null)
            val party = matcher.group(2)?.trim()
            return Triple(TransactionType.RECEIVED, amount, party)
        }
        
        matcher = sentPattern.matcher(body)
        if (matcher.find()) {
            val amount = parseAmount(matcher.group(1) ?: return null)
            val party = matcher.group(2)?.trim()
            return Triple(TransactionType.SENT, amount, party)
        }
        
        paymentPattern?.let {
            matcher = it.matcher(body)
            if (matcher.find()) {
                val amount = parseAmount(matcher.group(1) ?: return null)
                val party = matcher.group(2)?.trim()
                return Triple(TransactionType.PAYMENT, amount, party)
            }
        }
        
        return null
    }
    
    /**
     * Extract balance from message.
     */
    private fun extractBalance(body: String, pattern: Pattern): Double? {
        val matcher = pattern.matcher(body)
        return if (matcher.find()) {
            parseAmount(matcher.group(1) ?: return null)
        } else null
    }
    
    /**
     * Extract transaction ID from message.
     */
    private fun extractTransactionId(body: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(body)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else null
    }
    
    /**
     * Parse amount string to Double.
     */
    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").toDoubleOrNull() ?: 0.0
    }
    
    /**
     * Check if a message is a Mobile Money message.
     */
    fun isMobileMoneyMessage(sender: String, body: String): Boolean {
        return detectProvider(sender, body) != null ||
                body.contains("GHS", ignoreCase = true) ||
                body.contains("GHC", ignoreCase = true) ||
                (MTN_KEYWORDS + VODAFONE_KEYWORDS + AIRTELTIGO_KEYWORDS).any { 
                    body.contains(it, ignoreCase = true) 
                }
    }
}
