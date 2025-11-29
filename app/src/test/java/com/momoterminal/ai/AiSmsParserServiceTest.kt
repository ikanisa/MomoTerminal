package com.momoterminal.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for AiParsedTransaction data class.
 */
class AiParsedTransactionTest {

    @Test
    fun `getDisplayAmount converts pesewas to main unit correctly`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 5000L,
            currency = "GHS",
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(transaction.getDisplayAmount()).isWithin(0.01).of(50.0)
    }

    @Test
    fun `getDisplayAmount handles zero amount`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 0L,
            currency = "GHS",
            transactionType = "UNKNOWN",
            provider = "UNKNOWN",
            rawMessage = "Test message"
        )
        
        assertThat(transaction.getDisplayAmount()).isWithin(0.01).of(0.0)
    }

    @Test
    fun `getDisplayAmount handles small amounts`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 50L,
            currency = "GHS",
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(transaction.getDisplayAmount()).isWithin(0.01).of(0.50)
    }

    @Test
    fun `getDisplayAmount handles large amounts`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 10000050L,
            currency = "GHS",
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(transaction.getDisplayAmount()).isWithin(0.01).of(100000.50)
    }

    @Test
    fun `getDisplayBalance returns null when balance is null`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 5000L,
            currency = "GHS",
            transactionType = "RECEIVED",
            provider = "MTN",
            balanceInPesewas = null,
            rawMessage = "Test message"
        )
        
        assertThat(transaction.getDisplayBalance()).isNull()
    }

    @Test
    fun `getDisplayBalance converts balance correctly`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 5000L,
            currency = "GHS",
            transactionType = "RECEIVED",
            provider = "MTN",
            balanceInPesewas = 150000L,
            rawMessage = "Test message"
        )
        
        assertThat(transaction.getDisplayBalance()).isWithin(0.01).of(1500.0)
    }

    @Test
    fun `transaction defaults to gemini parser`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(transaction.parsedBy).isEqualTo("gemini")
    }

    @Test
    fun `transaction preserves all fields`() {
        val transaction = AiParsedTransaction(
            amountInPesewas = 5000L,
            currency = "GHS",
            senderPhone = "0244123456",
            recipientPhone = null,
            transactionId = "TXN123",
            transactionType = "RECEIVED",
            provider = "MTN",
            balanceInPesewas = 100000L,
            rawMessage = "You received GHS 50.00",
            parsedBy = "regex"
        )
        
        assertThat(transaction.amountInPesewas).isEqualTo(5000L)
        assertThat(transaction.currency).isEqualTo("GHS")
        assertThat(transaction.senderPhone).isEqualTo("0244123456")
        assertThat(transaction.recipientPhone).isNull()
        assertThat(transaction.transactionId).isEqualTo("TXN123")
        assertThat(transaction.transactionType).isEqualTo("RECEIVED")
        assertThat(transaction.provider).isEqualTo("MTN")
        assertThat(transaction.balanceInPesewas).isEqualTo(100000L)
        assertThat(transaction.rawMessage).isEqualTo("You received GHS 50.00")
        assertThat(transaction.parsedBy).isEqualTo("regex")
    }
}

/**
 * Unit tests for GeminiConfig.
 */
class GeminiConfigTest {

    @Test
    fun `model name is correct`() {
        assertThat(GeminiConfig.MODEL_NAME).isEqualTo("gemini-1.5-flash")
    }

    @Test
    fun `transaction extraction prompt is not empty`() {
        assertThat(GeminiConfig.TRANSACTION_EXTRACTION_PROMPT).isNotEmpty()
    }

    @Test
    fun `prompt contains required fields`() {
        val prompt = GeminiConfig.TRANSACTION_EXTRACTION_PROMPT
        
        assertThat(prompt).contains("amount_in_pesewas")
        assertThat(prompt).contains("currency")
        assertThat(prompt).contains("sender_phone")
        assertThat(prompt).contains("recipient_phone")
        assertThat(prompt).contains("transaction_id")
        assertThat(prompt).contains("transaction_type")
        assertThat(prompt).contains("provider")
        assertThat(prompt).contains("balance_in_pesewas")
    }

    @Test
    fun `prompt contains valid transaction types`() {
        val prompt = GeminiConfig.TRANSACTION_EXTRACTION_PROMPT
        
        assertThat(prompt).contains("RECEIVED")
        assertThat(prompt).contains("SENT")
        assertThat(prompt).contains("PAYMENT")
    }

    @Test
    fun `prompt contains valid providers`() {
        val prompt = GeminiConfig.TRANSACTION_EXTRACTION_PROMPT
        
        assertThat(prompt).contains("MTN")
        assertThat(prompt).contains("VODAFONE")
        assertThat(prompt).contains("AIRTELTIGO")
    }
}
