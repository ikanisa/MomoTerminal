package com.momoterminal.feature.vending.domain.model

data class VendingMachine(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int? = null,
    val status: MachineStatus,
    val productId: String,
    val productName: String,
    val productSizeML: Int = 500,
    val price: Long,
    val currency: String = "XAF",
    val stockLevel: StockLevel,
    val imageUrl: String? = null
) {
    fun formattedPrice(): String {
        val major = price / 100
        val minor = price % 100
        return String.format("%,d.%02d", major, minor)
    }
    
    fun formattedDistance(): String? {
        return distanceMeters?.let {
            when {
                it < 1000 -> "${it}m away"
                else -> String.format("%.1fkm away", it / 1000.0)
            }
        }
    }
}

enum class MachineStatus {
    AVAILABLE,
    OFFLINE,
    MAINTENANCE
}

enum class StockLevel {
    HIGH,
    MEDIUM,
    LOW,
    OUT_OF_STOCK
}
