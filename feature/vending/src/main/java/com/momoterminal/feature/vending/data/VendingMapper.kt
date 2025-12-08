package com.momoterminal.feature.vending.data

import com.momoterminal.feature.vending.domain.model.*

object VendingMapper {
    fun mapMachine(dto: VendingMachineDto): VendingMachine {
        return VendingMachine(
            id = dto.id,
            name = dto.name,
            location = dto.location,
            latitude = dto.latitude,
            longitude = dto.longitude,
            distanceMeters = dto.distanceMeters,
            status = mapMachineStatus(dto.status),
            productId = dto.productId,
            productName = dto.productName,
            productCategory = mapProductCategory(dto.productCategory),
            servingSizeML = dto.servingSizeML,
            pricePerServing = dto.pricePerServing,
            currency = dto.currency,
            stockLevel = mapStockLevel(dto.stockLevel),
            imageUrl = dto.imageUrl,
            isAgeRestricted = dto.isAgeRestricted
        )
    }
    
    fun mapOrder(dto: VendingOrderDto): VendingOrder {
        return VendingOrder(
            id = dto.id,
            userId = dto.userId,
            machineId = dto.machineId,
            machineName = dto.machineName,
            machineLocation = dto.machineLocation,
            productName = dto.productName,
            productCategory = mapProductCategory(dto.productCategory),
            quantity = dto.quantity,
            servingSizeML = dto.servingSizeML,
            pricePerServing = dto.pricePerServing,
            totalAmount = dto.totalAmount,
            status = mapOrderStatus(dto.status),
            createdAt = dto.createdAt,
            code = dto.code?.let { mapCode(it, dto.id, dto.machineId) }
        )
    }
    
    fun mapCode(dto: VendingCodeDto, orderId: String, machineId: String): VendingCode {
        return VendingCode(
            code = dto.code,
            orderId = orderId,
            machineId = machineId,
            expiresAt = dto.expiresAt,
            usedAt = dto.usedAt,
            totalServes = dto.totalServes,
            remainingServes = dto.remainingServes,
            closedAt = dto.closedAt
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
        "CREATED" -> OrderStatus.CREATED
        "PAID" -> OrderStatus.PAID
        "CODE_ISSUED" -> OrderStatus.CODE_ISSUED
        "CODE_GENERATED" -> OrderStatus.CODE_ISSUED
        "IN_PROGRESS" -> OrderStatus.IN_PROGRESS
        "COMPLETED" -> OrderStatus.COMPLETED
        "DISPENSED" -> OrderStatus.COMPLETED
        "EXPIRED" -> OrderStatus.EXPIRED
        "REFUNDED" -> OrderStatus.REFUNDED
        "FAILED" -> OrderStatus.FAILED
        "PENDING" -> OrderStatus.CREATED
        else -> OrderStatus.FAILED
    }
    
    private fun mapProductCategory(category: String) = when (category.uppercase()) {
        "JUICE" -> ProductCategory.JUICE
        "HOT_COFFEE" -> ProductCategory.HOT_COFFEE
        "COCKTAIL" -> ProductCategory.COCKTAIL
        "ALCOHOL" -> ProductCategory.ALCOHOL
        "BEER" -> ProductCategory.BEER
        else -> ProductCategory.JUICE
    }
}
