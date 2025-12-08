package com.momoterminal.core.ai

import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SyncStatus
import com.momoterminal.feature.sms.MomoSmsParser
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Parser Chain with fallback logic.
 * 
 * Priority order:
 * 1. OpenAI GPT-3.5-turbo (PRIMARY) - 95-97% accuracy
 * 2. Google Gemini 1.5 Flash (FALLBACK) - 93-95% accuracy
 * 3. Regex Parser (FINAL FALLBACK) - 80-85% accuracy
 */
@Singleton
class AiParserChain @Inject constructor(
    private val openAiParser: OpenAiParser,
    private val geminiParser: GeminiParser,
    private val regexParser: MomoSmsParser
) {
    
    companion object {
        private const val TAG = "AiParserChain"
        private const val REGEX_CONFIDENCE_DEFAULT = 0.7f
    }
    
    /**
     * Parse SMS using the fallback chain: OpenAI → Gemini → Regex
     */
    suspend fun parse(sender: String, body: String): ParsedTransaction {
        // 1. Try OpenAI first (PRIMARY)
        if (AiConfig.isOpenAiEnabled) {
            try {
                Timber.d("$TAG: Trying OpenAI parser (PRIMARY)")
                val result = openAiParser.parse(sender, body)
                if (result != null) {
                    Timber.i("$TAG: ✅ Successfully parsed with OpenAI")
                    return result
                }
            } catch (e: Exception) {
                Timber.w(e, "$TAG: OpenAI parsing failed, trying Gemini")
            }
        } else {
            Timber.d("$TAG: OpenAI is disabled, skipping to Gemini")
        }
        
        // 2. Fallback to Gemini (SECONDARY)
        if (AiConfig.isGeminiEnabled) {
            try {
                Timber.d("$TAG: Trying Gemini parser (FALLBACK)")
                val result = geminiParser.parse(sender, body)
                if (result != null) {
                    Timber.i("$TAG: ✅ Successfully parsed with Gemini")
                    return result
                }
            } catch (e: Exception) {
                Timber.w(e, "$TAG: Gemini parsing failed, falling back to regex")
            }
        } else {
            Timber.d("$TAG: Gemini is disabled, skipping to Regex")
        }
        
        // 3. Final fallback to Regex
        Timber.d("$TAG: Using Regex parser (FINAL FALLBACK)")
        val regexResult = regexParser.parse(sender, body)
        
        return if (regexResult != null) {
            Timber.i("$TAG: ✅ Successfully parsed with Regex")
            ParsedTransaction(
                entity = regexResult.copy(
                    parsedBy = "regex",
                    aiConfidence = REGEX_CONFIDENCE_DEFAULT,
                    synced = false,
                    syncStatus = SyncStatus.PENDING
                ),
                parsedBy = "regex",
                confidence = REGEX_CONFIDENCE_DEFAULT
            )
        } else {
            // If even regex fails, return a minimal entity
            Timber.w("$TAG: ⚠️ All parsers failed, creating minimal entity")
            val minimalEntity = SmsTransactionEntity(
                rawMessage = body,
                sender = sender,
                amount = 0.0,
                currency = "GHS",
                type = com.momoterminal.core.database.entity.SmsTransactionType.UNKNOWN,
                balance = null,
                reference = null,
                timestamp = System.currentTimeMillis(),
                synced = false,
                syncStatus = SyncStatus.FAILED,
                parsedBy = "none",
                aiConfidence = 0.0f
            )
            ParsedTransaction(
                entity = minimalEntity,
                parsedBy = "none",
                confidence = 0.0f
            )
        }
    }
}
