package com.momoterminal

import com.momoterminal.api.PaymentTransaction
import com.momoterminal.api.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PaymentTransaction model.
 */
class PaymentTransactionTest {

    @Test
    fun `create transaction with required fields`() {
        val transaction = PaymentTransaction(
            amount = 100.00,
            senderNumber = "0244123456",
            transactionId = "TXN123456",
            rawMessage = "You have received GHS 100.00 from 0244123456"
        )
        
        assertEquals(100.00, transaction.amount, 0.01)
        assertEquals("0244123456", transaction.senderNumber)
        assertEquals("TXN123456", transaction.transactionId)
        assertEquals("GHS", transaction.currency)
        assertEquals(TransactionStatus.PENDING, transaction.status)
    }

    @Test
    fun `transaction has default currency GHS`() {
        val transaction = PaymentTransaction(
            amount = 50.00,
            senderNumber = "0201234567",
            transactionId = "TX001",
            rawMessage = "Payment received"
        )
        
        assertEquals("GHS", transaction.currency)
    }

    @Test
    fun `transaction has default status PENDING`() {
        val transaction = PaymentTransaction(
            amount = 25.00,
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
            amount = 10.00,
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
            amount = 200.00,
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
            amount = 50.00,
            senderNumber = "0244333333",
            transactionId = "TX005",
            rawMessage = "Payment",
            status = TransactionStatus.CONFIRMED
        )
        
        assertEquals(TransactionStatus.CONFIRMED, transaction.status)
    }
}
