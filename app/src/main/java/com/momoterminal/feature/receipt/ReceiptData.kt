package com.momoterminal.feature.receipt

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enum representing transaction types.
 */
enum class TransactionType {
    PAYMENT,
    TRANSFER,
    WITHDRAWAL,
    DEPOSIT,
    AIRTIME,
    BILL_PAYMENT
}

/**
 * Enum representing transaction status.
 */
enum class TransactionStatus {
    SUCCESS,
    PENDING,
    FAILED,
    CANCELLED
}

/**
 * Data class representing merchant information.
 */
data class MerchantInfo(
    val name: String,
    val code: String,
    val address: String? = null,
    val phone: String? = null
)

/**
 * Data class representing complete receipt data for a transaction.
 */
data class ReceiptData(
    val transactionId: String,
    val referenceNumber: String,
    val type: TransactionType,
    val status: TransactionStatus,
    val amount: Double,
    val fee: Double = 0.0,
    val timestamp: Date,
    val senderName: String,
    val senderPhone: String,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val description: String? = null,
    val merchantInfo: MerchantInfo? = null,
    val currency: String = "GHS"
) {
    /**
     * Total amount including fees.
     */
    val totalAmount: Double
        get() = amount + fee
    
    /**
     * Formatted amount string with currency.
     */
    val formattedAmount: String
        get() = formatCurrency(amount)
    
    /**
     * Formatted fee string with currency.
     */
    val formattedFee: String
        get() = formatCurrency(fee)
    
    /**
     * Formatted total amount string with currency.
     */
    val formattedTotal: String
        get() = formatCurrency(totalAmount)
    
    /**
     * Formatted date string.
     */
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(timestamp)
    
    /**
     * Formatted time string.
     */
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(timestamp)
    
    /**
     * Formatted date and time string.
     */
    val formattedDateTime: String
        get() = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(timestamp)
    
    /**
     * Human-readable transaction type.
     */
    val typeDisplayName: String
        get() = when (type) {
            TransactionType.PAYMENT -> "Payment"
            TransactionType.TRANSFER -> "Transfer"
            TransactionType.WITHDRAWAL -> "Withdrawal"
            TransactionType.DEPOSIT -> "Deposit"
            TransactionType.AIRTIME -> "Airtime Purchase"
            TransactionType.BILL_PAYMENT -> "Bill Payment"
        }
    
    /**
     * Human-readable status.
     */
    val statusDisplayName: String
        get() = when (status) {
            TransactionStatus.SUCCESS -> "Successful"
            TransactionStatus.PENDING -> "Pending"
            TransactionStatus.FAILED -> "Failed"
            TransactionStatus.CANCELLED -> "Cancelled"
        }
    
    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "GH")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }.format(value)
    }
    
    companion object {
        /**
         * Create sample receipt data for testing.
         */
        fun createSample(): ReceiptData {
            return ReceiptData(
                transactionId = "TXN-2024-123456",
                referenceNumber = "REF-789012",
                type = TransactionType.PAYMENT,
                status = TransactionStatus.SUCCESS,
                amount = 150.00,
                fee = 1.50,
                timestamp = Date(),
                senderName = "John Doe",
                senderPhone = "+233 24 123 4567",
                recipientName = "Shop ABC",
                recipientPhone = "+233 20 987 6543",
                description = "Payment for goods",
                merchantInfo = MerchantInfo(
                    name = "Shop ABC",
                    code = "MERCHANT123",
                    address = "123 Main Street, Accra",
                    phone = "+233 20 987 6543"
                )
            )
        }
    }
}
