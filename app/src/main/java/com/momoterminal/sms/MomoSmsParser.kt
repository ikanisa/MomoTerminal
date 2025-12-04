package com.momoterminal.sms

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SmsTransactionType
import javax.inject.Inject
import javax.inject.Singleton

interface SmsParserInterface {
    fun isMomoMessage(sender: String, body: String): Boolean
    fun parse(sender: String, body: String): SmsTransactionEntity?
}

@Singleton
class MomoSmsParser @Inject constructor() : SmsParserInterface {
    
    // Known MoMo senders across African markets
    private val momoSenders = listOf(
        // Ghana
        "MobileMoney", "MTN", "MOMO", "Vodafone", "AirtelTigo",
        // Rwanda
        "M-Money", "MTN Rwanda", "Airtel Rwanda",
        // DRC
        "M-Pesa", "Orange Money", "Airtel Money",
        // Tanzania
        "M-PESA", "Tigo Pesa", "Airtel Money", "Halotel",
        // Burundi
        "Lumicash", "EcoCash",
        // Zambia
        "MTN MoMo", "Airtel Money", "Zamtel Kwacha",
        // Generic
        "Orange", "Airtel"
    )

    // Currency patterns
    private val currencyPattern = """([A-Z]{3})\s*([\d,]+\.?\d*)""".toRegex()
    private val amountFirstPattern = """([\d,]+\.?\d*)\s*([A-Z]{3})""".toRegex()
    
    // Balance patterns
    private val balancePatterns = listOf(
        """balance[:\s]+([A-Z]{3})\s*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE),
        """bal[:\s]+([A-Z]{3})\s*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE),
        """new balance[:\s]*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE),
        """available[:\s]*([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
    )
    
    // Reference/Transaction ID patterns
    private val refPatterns = listOf(
        """(?:ref|id|txn|transaction)[:\s#]*([A-Z0-9]+)""".toRegex(RegexOption.IGNORE_CASE),
        """(?:confirmation|receipt)[:\s#]*([A-Z0-9]+)""".toRegex(RegexOption.IGNORE_CASE),
        """\b([A-Z]{2,4}\d{8,12})\b""".toRegex() // Common format: XX12345678
    )

    // Transaction type keywords
    private val receivedKeywords = listOf("received", "credited", "deposit", "incoming", "from")
    private val sentKeywords = listOf("sent", "transferred", "paid", "payment to", "debited")
    private val cashOutKeywords = listOf("cash out", "withdrawn", "withdrawal", "atm")
    private val airtimeKeywords = listOf("airtime", "recharge", "top-up", "topup")
    private val depositKeywords = listOf("deposit", "cash in")

    override fun isMomoMessage(sender: String, body: String): Boolean {
        val senderMatch = momoSenders.any { sender.contains(it, ignoreCase = true) }
        val hasAmount = currencyPattern.containsMatchIn(body) || amountFirstPattern.containsMatchIn(body)
        val hasMomoKeyword = listOf("momo", "mobile money", "m-pesa", "mpesa", "money").any { 
            body.contains(it, ignoreCase = true) 
        }
        return senderMatch || (hasAmount && hasMomoKeyword)
    }

    override fun parse(sender: String, body: String): SmsTransactionEntity? {
        if (!isMomoMessage(sender, body)) return null

        val lowerBody = body.lowercase()
        
        // Determine transaction type
        val type = when {
            receivedKeywords.any { lowerBody.contains(it) } -> SmsTransactionType.RECEIVED
            sentKeywords.any { lowerBody.contains(it) } -> SmsTransactionType.SENT
            cashOutKeywords.any { lowerBody.contains(it) } -> SmsTransactionType.CASH_OUT
            airtimeKeywords.any { lowerBody.contains(it) } -> SmsTransactionType.AIRTIME
            depositKeywords.any { lowerBody.contains(it) } -> SmsTransactionType.DEPOSIT
            else -> SmsTransactionType.UNKNOWN
        }

        // Extract amount and currency
        val (amount, currency) = extractAmount(body)
        
        // Extract balance
        val balance = extractBalance(body)
        
        // Extract reference
        val reference = extractReference(body)

        return SmsTransactionEntity(
            rawMessage = body,
            sender = sender,
            amount = amount,
            currency = currency,
            type = type,
            balance = balance,
            reference = reference
        )
    }

    private fun extractAmount(body: String): Pair<Double, String> {
        currencyPattern.find(body)?.let { match ->
            val currency = match.groupValues[1]
            val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
            return amount to currency
        }
        
        amountFirstPattern.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
            val currency = match.groupValues[2]
            return amount to currency
        }
        
        return 0.0 to "GHS"
    }

    private fun extractBalance(body: String): Double? {
        for (pattern in balancePatterns) {
            pattern.find(body)?.let { match ->
                val amountStr = if (match.groupValues.size > 2) {
                    match.groupValues[2]
                } else {
                    match.groupValues[1]
                }
                return amountStr.replace(",", "").toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractReference(body: String): String? {
        for (pattern in refPatterns) {
            pattern.find(body)?.let { match ->
                return match.groupValues[1]
            }
        }
        return null
    }
}
