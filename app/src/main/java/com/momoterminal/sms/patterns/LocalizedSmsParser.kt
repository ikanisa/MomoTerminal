package com.momoterminal.sms.patterns

/**
 * Simplified localized SMS parser that can be extended with country-specific patterns.
 */
class LocalizedSmsParser(
    @Suppress("unused") private val registry: SmsPatternRegistry
) {
    data class ParseResult(
        val amountInPesewas: Long,
        val currency: String,
        val senderOrRecipient: String?,
        val transactionId: String?,
        val provider: String?,
        val rawMessage: String
    )

    fun parse(sender: String, body: String): ParseResult? {
        // Placeholder implementation - extend with real patterns as needed
        return null
    }
}
