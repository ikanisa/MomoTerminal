package com.momoterminal.feature.sms

import com.momoterminal.core.ai.RegexSmsParser
import com.momoterminal.core.database.entity.SmsTransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SMS parser interface for checking if messages are MoMo transactions.
 */
interface SmsParserInterface {
    fun isMomoMessage(sender: String, body: String): Boolean
    fun parse(sender: String, body: String): SmsTransactionEntity?
}

/**
 * Mobile Money SMS Parser - delegates to RegexSmsParser from core:ai.
 * This class exists for backward compatibility with existing code.
 */
@Singleton
class MomoSmsParser @Inject constructor(
    private val regexParser: RegexSmsParser
) : SmsParserInterface {
    
    override fun isMomoMessage(sender: String, body: String): Boolean {
        return regexParser.isMomoMessage(sender, body)
    }

    override fun parse(sender: String, body: String): SmsTransactionEntity? {
        return regexParser.parse(sender, body)
    }
}
