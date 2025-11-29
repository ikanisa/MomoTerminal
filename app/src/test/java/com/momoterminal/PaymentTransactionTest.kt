package com.momoterminal

import com.momoterminal.api.PaymentTransaction
import com.momoterminal.api.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PaymentTransaction model.
 * 
 * Note: Amounts are stored in pesewas (smallest currency unit) to avoid
 * floating-point precision errors. 1 GHS = 100 pesewas.
 */
class PaymentTransactionTest {

    @Test
    fun `create transaction with required fields`() {
        val transaction = PaymentTransaction(
            amountInPesewas = 10000, // 100.00 GHS
            senderNumber = "0244123456",
            transactionId = "TXN123456",
            rawMessage = "You have received GHS 100.00 from 0244123456"
        )
        
        assertEquals(10000, transaction.amountInPesewas)
        assertEquals(100.00, transaction.getDisplayAmount(), 0.01)
        assertEquals("0244123456", transaction.senderNumber)
        assertEquals("TXN123456", transaction.transactionId)
        assertEquals("GHS", transaction.currency)
        assertEquals(TransactionStatus.PENDING, transaction.status)
    }

    @Test
    fun `transaction has default currency GHS`() {
        val transaction = PaymentTransaction(
            amountInPesewas = 5000, // 50.00 GHS
            senderNumber = "0201234567",
            transactionId = "TX001",
            rawMessage = "Payment received"
        )
        
        assertEquals("GHS", transaction.currency)
    }

    @Test
    fun `transaction has default status PENDING`() {
        val transaction = PaymentTransaction(
            amountInPesewas = 2500, // 25.00 GHS
            senderNumber = "0271234567",
            transactionId = "TX002",
            rawMessage = "Payment received"
        )
        
        assertEquals(TransactionStatus.PENDING, transaction.status)
    }

    @Test
    fun `transaction timestamp is set on creation`() {
        val beforeTime = System.currentTimeMillis()
        
        val transaction = PaymentTransaction(
            amountInPesewas = 1000, // 10.00 GHS
            senderNumber = "0241234567",
            transactionId = "TX003",
            rawMessage = "Test"
        )
        
        val afterTime = System.currentTimeMillis()
        
        assertTrue(transaction.timestamp >= beforeTime)
        assertTrue(transaction.timestamp <= afterTime)
    }

    @Test
    fun `transaction can have optional fields`() {
        val transaction = PaymentTransaction(
            amountInPesewas = 20000, // 200.00 GHS
            senderNumber = "0244111111",
            transactionId = "TX004",
            rawMessage = "Payment",
            recipientNumber = "0244222222",
            merchantCode = "MERCH001"
        )
        
        assertEquals("0244222222", transaction.recipientNumber)
        assertEquals("MERCH001", transaction.merchantCode)
    }

    @Test
    fun `transaction status can be set`() {
        val transaction = PaymentTransaction(
            amountInPesewas = 5000, // 50.00 GHS
            senderNumber = "0244333333",
            transactionId = "TX005",
            rawMessage = "Payment",
            status = TransactionStatus.CONFIRMED
        )
        
        assertEquals(TransactionStatus.CONFIRMED, transaction.status)
    }
    
    @Test
    fun `toPesewas converts double to long correctly`() {
        assertEquals(10000L, PaymentTransaction.toPesewas(100.00))
        assertEquals(5050L, PaymentTransaction.toPesewas(50.50))
        assertEquals(1L, PaymentTransaction.toPesewas(0.01))
    }
    
    @Test
    fun `getDisplayAmount converts pesewas to double correctly`() {
        val transaction = PaymentTransaction(
            amountInPesewas = 12345, // 123.45 GHS
            senderNumber = "0244123456",
            transactionId = "TX006",
            rawMessage = "Test"
        )
        
        assertEquals(123.45, transaction.getDisplayAmount(), 0.001)
    }
}
