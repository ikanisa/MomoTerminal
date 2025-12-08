package com.momoterminal.core.ai

import com.momoterminal.core.database.entity.SmsTransactionEntity

/**
 * Common interface for AI-powered SMS parsers.
 */
interface AiParserInterface {
    /**
     * Parse an SMS message into a structured transaction entity.
     * 
     * @param sender The SMS sender address
     * @param body The SMS body text
     * @return Parsed transaction entity if successful, null otherwise
     */
    suspend fun parse(sender: String, body: String): ParsedTransaction?
}

/**
 * Result of parsing an SMS message.
 */
data class ParsedTransaction(
    val entity: SmsTransactionEntity,
    val parsedBy: String,  // "openai", "gemini", or "regex"
    val confidence: Float  // 0.0 to 1.0
)
