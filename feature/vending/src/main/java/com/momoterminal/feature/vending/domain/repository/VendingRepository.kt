package com.momoterminal.feature.vending.domain.repository

import com.momoterminal.feature.vending.domain.model.VendingMachine
import com.momoterminal.feature.vending.domain.model.VendingOrder
import kotlinx.coroutines.flow.Flow

interface VendingRepository {
    fun getMachines(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Int? = null
    ): Flow<Result<List<VendingMachine>>>
    
    suspend fun getMachineById(machineId: String): Result<VendingMachine>
    
    suspend fun createOrder(
        machineId: String,
        quantity: Int
    ): Result<VendingOrder>
    
    fun getOrders(): Flow<Result<List<VendingOrder>>>
    
    suspend fun getOrderById(orderId: String): Result<VendingOrder>
    
    suspend fun cancelOrder(orderId: String): Result<Unit>
    
    suspend fun refreshOrderStatus(orderId: String): Result<VendingOrder>
}
