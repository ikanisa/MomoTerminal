package com.momoterminal.feature.vending.domain.model

data class VendingOrder(
    val id: String,
    val userId: String,
    val machineId: String,
    val machineName: String,
    val machineLocation: String,
    val productName: String,
    val productSizeML: Int,
    val amount: Long,
    val status: OrderStatus,
    val createdAt: Long,
    val code: VendingCode? = null
) {
    fun formattedAmount(): String {
        val major = amount / 100
        val minor = amount % 100
        return String.format("%,d.%02d", major, minor)
    }
    
    fun formattedDate(): String {
        val date = java.util.Date(createdAt)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}

enum class OrderStatus {
    PENDING,
    CODE_GENERATED,
    DISPENSED,
    EXPIRED,
    REFUNDED,
    FAILED
}
