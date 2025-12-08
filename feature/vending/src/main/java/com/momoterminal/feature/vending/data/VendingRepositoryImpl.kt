package com.momoterminal.feature.vending.data

import com.momoterminal.feature.vending.domain.model.VendingMachine
import com.momoterminal.feature.vending.domain.model.VendingOrder
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VendingRepositoryImpl @Inject constructor(
    private val apiService: VendingApiService
) : VendingRepository {
    
    override fun getMachines(
        latitude: Double?,
        longitude: Double?,
        radiusKm: Int?
    ): Flow<Result<List<VendingMachine>>> = flow {
        try {
            val response = apiService.getMachines(latitude, longitude, radiusKm)
            if (response.isSuccessful && response.body() != null) {
                val machines = response.body()!!.machines.map { VendingMapper.mapMachine(it) }
                emit(Result.success(machines))
            } else {
                emit(Result.failure(Exception("Failed to fetch machines: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun getMachineById(machineId: String): Result<VendingMachine> {
        return try {
            val response = apiService.getMachineById(machineId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(VendingMapper.mapMachine(response.body()!!))
            } else {
                Result.failure(Exception("Failed to fetch machine: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createOrder(
        machineId: String,
        amount: Long
    ): Result<VendingOrder> {
        return try {
            val request = CreateOrderRequest(machineId, amount)
            val response = apiService.createOrder(request)
            if (response.isSuccessful && response.body() != null) {
                val orderDto = response.body()!!.order.copy(
                    code = response.body()!!.code,
                    code_expires_at = response.body()!!.code_expires_at
                )
                Result.success(VendingMapper.mapOrder(orderDto))
            } else {
                Result.failure(Exception("Failed to create order: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getOrders(): Flow<Result<List<VendingOrder>>> = flow {
        try {
            val response = apiService.getOrders()
            if (response.isSuccessful && response.body() != null) {
                val orders = response.body()!!.orders.map { VendingMapper.mapOrder(it) }
                emit(Result.success(orders))
            } else {
                emit(Result.failure(Exception("Failed to fetch orders: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun getOrderById(orderId: String): Result<VendingOrder> {
        return try {
            val response = apiService.getOrderById(orderId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(VendingMapper.mapOrder(response.body()!!))
            } else {
                Result.failure(Exception("Failed to fetch order: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelOrder(orderId: String): Result<Unit> {
        return try {
            val response = apiService.cancelOrder(orderId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to cancel order: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun refreshOrderStatus(orderId: String): Result<VendingOrder> {
        return getOrderById(orderId)
    }
}
