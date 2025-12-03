package com.momoterminal.feature.sms.patterns

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses SMS messages using localized patterns from the registry.
 */
@Singleton
class LocalizedSmsParser @Inject constructor(
    private val patternRegistry: SmsPatternRegistry
) {
    data class ParsedTransaction(
        val amount: Double,
        val currency: String,
        val party: String,
        val transactionId: String?,
        val balance: Double?,
        val isReceived: Boolean,
        val providerId: String,
        val providerName: String
    )

    fun parse(countryCode: String, sender: String, message: String): ParsedTransaction? {
        val provider = patternRegistry.detectProvider(countryCode, sender) ?: return null
        
        // Try received pattern
        provider.receivedPattern.matcher(message).let { matcher ->
            if (matcher.find()) {
                return ParsedTransaction(
                    amount = parseAmount(matcher.group(provider.amountGroup) ?: "0"),
                    currency = provider.currencyCode,
                    party = matcher.group(provider.partyGroup)?.trim() ?: "",
                    transactionId = extractTransactionId(provider, message),
                    balance = extractBalance(provider, message),
                    isReceived = true,
                    providerId = provider.providerId,
                    providerName = provider.providerName
                )
            }
        }
        
        // Try sent pattern
        provider.sentPattern.matcher(message).let { matcher ->
            if (matcher.find()) {
                return ParsedTransaction(
                    amount = parseAmount(matcher.group(provider.amountGroup) ?: "0"),
                    currency = provider.currencyCode,
                    party = matcher.group(provider.partyGroup)?.trim() ?: "",
                    transactionId = extractTransactionId(provider, message),
                    balance = extractBalance(provider, message),
                    isReceived = false,
                    providerId = provider.providerId,
                    providerName = provider.providerName
                )
            }
        }
        
        return null
    }

    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").replace(" ", "").toDoubleOrNull() ?: 0.0
    }

    private fun extractTransactionId(provider: SmsPatternRegistry.ProviderPatterns, message: String): String? {
        return provider.transactionIdPattern?.matcher(message)?.let { matcher ->
            if (matcher.find()) matcher.group(1) else null
        }
    }

    private fun extractBalance(provider: SmsPatternRegistry.ProviderPatterns, message: String): Double? {
        return provider.balancePattern?.matcher(message)?.let { matcher ->
            if (matcher.find()) parseAmount(matcher.group(1) ?: "0") else null
        }
    }
}
