package com.momoterminal.feature.vending.data

import com.momoterminal.feature.vending.domain.model.*

object VendingMapper {
    fun mapMachine(dto: VendingMachineDto) = VendingMachine(
        id = dto.id, name = dto.name, location = dto.location,
        latitude = dto.latitude, longitude = dto.longitude,
        distanceMeters = dto.distanceMeters, status = mapMachineStatus(dto.status),
        productId = dto.productId, productName = dto.productName,
        productSizeML = dto.productSizeML, price = dto.price, currency = dto.currency,
        stockLevel = mapStockLevel(dto.stockLevel), imageUrl = dto.imageUrl
    )
    
    fun mapOrder(dto: VendingOrderDto): VendingOrder {
        val code = if (dto.code != null && dto.codeExpiresAt != null) {
            VendingCode(dto.code, dto.id, dto.machineId, dto.codeExpiresAt, dto.codeUsedAt)
        } else null
        return VendingOrder(
            dto.id, dto.userId, dto.machineId, dto.machineName, dto.machineLocation,
            dto.productName, dto.productSizeML, dto.amount, mapOrderStatus(dto.status),
            dto.createdAt, code
        )
    }
    
    private fun mapMachineStatus(status: String) = when (status.uppercase()) {
        "AVAILABLE" -> MachineStatus.AVAILABLE
        "OFFLINE" -> MachineStatus.OFFLINE
        "MAINTENANCE" -> MachineStatus.MAINTENANCE
        else -> MachineStatus.OFFLINE
    }
    
    private fun mapStockLevel(level: String) = when (level.uppercase()) {
        "HIGH" -> StockLevel.HIGH
        "MEDIUM" -> StockLevel.MEDIUM
        "LOW" -> StockLevel.LOW
        "OUT_OF_STOCK" -> StockLevel.OUT_OF_STOCK
        else -> StockLevel.MEDIUM
    }
    
    private fun mapOrderStatus(status: String) = when (status.uppercase()) {
        "PENDING" -> OrderStatus.PENDING
        "CODE_GENERATED" -> OrderStatus.CODE_GENERATED
        "DISPENSED" -> OrderStatus.DISPENSED
        "EXPIRED" -> OrderStatus.EXPIRED
        "REFUNDED" -> OrderStatus.REFUNDED
        "FAILED" -> OrderStatus.FAILED
        else -> OrderStatus.PENDING
    }
}
