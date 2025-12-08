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
            distanceMeters = dto.distance_meters,
            status = mapMachineStatus(dto.status),
            productId = dto.product_id,
            productName = dto.product_name,
            productSizeML = dto.product_size_ml,
            price = dto.price,
            stockLevel = mapStockLevel(dto.stock_level),
            imageUrl = dto.image_url
        )
    }
    
    fun mapOrder(dto: VendingOrderDto): VendingOrder {
        val code = if (dto.code != null && dto.code_expires_at != null) {
            VendingCode(
                code = dto.code,
                orderId = dto.id,
                machineId = dto.machine_id,
                expiresAt = dto.code_expires_at,
                usedAt = dto.code_used_at
            )
        } else null
        
        return VendingOrder(
            id = dto.id,
            userId = dto.user_id,
            machineId = dto.machine_id,
            machineName = dto.machine_name,
            machineLocation = dto.machine_location,
            productName = dto.product_name,
            productSizeML = dto.product_size_ml,
            amount = dto.amount,
            status = mapOrderStatus(dto.status),
            createdAt = dto.created_at,
            code = code
        )
    }
    
    private fun mapMachineStatus(status: String): MachineStatus {
        return when (status.uppercase()) {
            "AVAILABLE" -> MachineStatus.AVAILABLE
            "OFFLINE" -> MachineStatus.OFFLINE
            "MAINTENANCE" -> MachineStatus.MAINTENANCE
            else -> MachineStatus.OFFLINE
        }
    }
    
    private fun mapStockLevel(level: String): StockLevel {
        return when (level.uppercase()) {
            "HIGH" -> StockLevel.HIGH
            "MEDIUM" -> StockLevel.MEDIUM
            "LOW" -> StockLevel.LOW
            "OUT_OF_STOCK" -> StockLevel.OUT_OF_STOCK
            else -> StockLevel.MEDIUM
        }
    }
    
    private fun mapOrderStatus(status: String): OrderStatus {
        return when (status.uppercase()) {
            "PENDING" -> OrderStatus.PENDING
            "CODE_GENERATED" -> OrderStatus.CODE_GENERATED
            "DISPENSED" -> OrderStatus.DISPENSED
            "EXPIRED" -> OrderStatus.EXPIRED
            "REFUNDED" -> OrderStatus.REFUNDED
            "FAILED" -> OrderStatus.FAILED
            else -> OrderStatus.PENDING
        }
    }
}
