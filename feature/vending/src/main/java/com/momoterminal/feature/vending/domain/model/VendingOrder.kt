package com.momoterminal.feature.vending.domain.model

data class VendingOrder(
    val id: String,
    val userId: String,
    val machineId: String,
    val machineName: String,
    val machineLocation: String,
    val productName: String,
    val productCategory: ProductCategory,
    val quantity: Int,
    val servingSizeML: Int = 500,
    val pricePerServing: Long,
    val totalAmount: Long,
    val status: OrderStatus,
    val createdAt: Long,
    val code: VendingCode? = null
) {
    fun formattedAmount(): String {
        val major = totalAmount / 100
        val minor = totalAmount % 100
        return String.format("%,d.%02d", major, minor)
    }
    
    fun formattedDate(): String {
        val date = java.util.Date(createdAt)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
    
    fun quantityLabel(): String = "$quantity cup${if (quantity > 1) "s" else ""} (${quantity * servingSizeML}ml)"
}

enum class OrderStatus {
    CREATED,
    PAID,
    CODE_ISSUED,
    IN_PROGRESS,
    COMPLETED,
    EXPIRED,
    REFUNDED,
    FAILED
}
