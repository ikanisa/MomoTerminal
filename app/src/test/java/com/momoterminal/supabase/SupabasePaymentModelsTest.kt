package com.momoterminal.supabase

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for SupabasePayment and related models.
 */
class SupabasePaymentModelsTest {

    @Test
    fun `getDisplayAmount converts pesewas correctly`() {
        val payment = SupabasePayment(
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(payment.getDisplayAmount()).isWithin(0.01).of(50.0)
    }

    @Test
    fun `getDisplayAmount handles zero amount`() {
        val payment = SupabasePayment(
            amountInPesewas = 0L,
            transactionType = "UNKNOWN",
            provider = "UNKNOWN",
            rawMessage = "Test message"
        )
        
        assertThat(payment.getDisplayAmount()).isWithin(0.01).of(0.0)
    }

    @Test
    fun `getDisplayBalance returns null when balance is null`() {
        val payment = SupabasePayment(
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            balanceInPesewas = null,
            rawMessage = "Test message"
        )
        
        assertThat(payment.getDisplayBalance()).isNull()
    }

    @Test
    fun `getDisplayBalance converts balance correctly`() {
        val payment = SupabasePayment(
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            balanceInPesewas = 150000L,
            rawMessage = "Test message"
        )
        
        assertThat(payment.getDisplayBalance()).isWithin(0.01).of(1500.0)
    }

    @Test
    fun `payment defaults have correct values`() {
        val payment = SupabasePayment(
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(payment.id).isNull()
        assertThat(payment.currency).isEqualTo("GHS")
        assertThat(payment.parsedBy).isEqualTo("gemini")
        assertThat(payment.createdAt).isNull()
        assertThat(payment.syncedAt).isNull()
    }

    @Test
    fun `payment preserves all fields`() {
        val payment = SupabasePayment(
            id = "test-id-123",
            amountInPesewas = 5000L,
            currency = "GHS",
            senderPhone = "0244123456",
            recipientPhone = "0201234567",
            transactionId = "TXN123",
            transactionType = "RECEIVED",
            provider = "MTN",
            balanceInPesewas = 100000L,
            rawMessage = "You received GHS 50.00",
            deviceId = "Pixel 6",
            merchantCode = "MERCHANT001",
            parsedBy = "regex",
            localId = 42L,
            createdAt = "2024-01-01T12:00:00Z",
            syncedAt = "2024-01-01T12:00:01Z"
        )
        
        assertThat(payment.id).isEqualTo("test-id-123")
        assertThat(payment.amountInPesewas).isEqualTo(5000L)
        assertThat(payment.currency).isEqualTo("GHS")
        assertThat(payment.senderPhone).isEqualTo("0244123456")
        assertThat(payment.recipientPhone).isEqualTo("0201234567")
        assertThat(payment.transactionId).isEqualTo("TXN123")
        assertThat(payment.transactionType).isEqualTo("RECEIVED")
        assertThat(payment.provider).isEqualTo("MTN")
        assertThat(payment.balanceInPesewas).isEqualTo(100000L)
        assertThat(payment.rawMessage).isEqualTo("You received GHS 50.00")
        assertThat(payment.deviceId).isEqualTo("Pixel 6")
        assertThat(payment.merchantCode).isEqualTo("MERCHANT001")
        assertThat(payment.parsedBy).isEqualTo("regex")
        assertThat(payment.localId).isEqualTo(42L)
        assertThat(payment.createdAt).isEqualTo("2024-01-01T12:00:00Z")
        assertThat(payment.syncedAt).isEqualTo("2024-01-01T12:00:01Z")
    }

    @Test
    fun `payment insert has correct defaults`() {
        val insert = SupabasePaymentInsert(
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        assertThat(insert.currency).isEqualTo("GHS")
        assertThat(insert.parsedBy).isEqualTo("gemini")
        assertThat(insert.senderPhone).isNull()
        assertThat(insert.recipientPhone).isNull()
        assertThat(insert.transactionId).isNull()
        assertThat(insert.balanceInPesewas).isNull()
        assertThat(insert.deviceId).isNull()
        assertThat(insert.merchantCode).isNull()
        assertThat(insert.localId).isNull()
    }

    @Test
    fun `PaymentResult Success contains payment`() {
        val payment = SupabasePayment(
            id = "test-id",
            amountInPesewas = 5000L,
            transactionType = "RECEIVED",
            provider = "MTN",
            rawMessage = "Test message"
        )
        
        val result = PaymentResult.Success(payment)
        
        assertThat(result.payment).isEqualTo(payment)
    }

    @Test
    fun `PaymentResult Error contains message`() {
        val exception = RuntimeException("Test error")
        val result = PaymentResult.Error("Failed to insert", exception)
        
        assertThat(result.message).isEqualTo("Failed to insert")
        assertThat(result.exception).isEqualTo(exception)
    }

    @Test
    fun `PaymentResult Error works without exception`() {
        val result = PaymentResult.Error("Simple error")
        
        assertThat(result.message).isEqualTo("Simple error")
        assertThat(result.exception).isNull()
    }
}
