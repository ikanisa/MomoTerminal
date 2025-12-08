package com.momoterminal.feature.vending.data

import com.momoterminal.feature.vending.domain.model.*
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class VendingRepositoryImpl @Inject constructor(
    private val apiService: VendingApiService
) : VendingRepository {
    
    override fun getMachines(latitude: Double?, longitude: Double?, radiusKm: Int?) = flow {
        try {
            val response = apiService.getMachines(latitude, longitude, radiusKm)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!.machines.map { VendingMapper.mapMachine(it) }))
            } else {
                emit(Result.failure(Exception("Failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    override suspend fun getMachineById(machineId: String) = try {
        val response = apiService.getMachineById(machineId)
        if (response.isSuccessful && response.body() != null) {
            Result.success(VendingMapper.mapMachine(response.body()!!))
        } else Result.failure(Exception("Failed: ${response.message()}"))
    } catch (e: Exception) { Result.failure(e) }
    
    override suspend fun createOrder(machineId: String, amount: Long) = try {
        val response = apiService.createOrder(CreateOrderRequest(machineId, amount))
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            Result.success(VendingMapper.mapOrder(body.order.copy(code = body.code, codeExpiresAt = body.codeExpiresAt)))
        } else Result.failure(Exception("Failed: ${response.message()}"))
    } catch (e: Exception) { Result.failure(e) }
    
    override fun getOrders() = flow {
        try {
            val response = apiService.getOrders()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.success(response.body()!!.orders.map { VendingMapper.mapOrder(it) }))
            } else emit(Result.failure(Exception("Failed: ${response.message()}")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }
    
    override suspend fun getOrderById(orderId: String) = try {
        val response = apiService.getOrderById(orderId)
        if (response.isSuccessful && response.body() != null) {
            Result.success(VendingMapper.mapOrder(response.body()!!))
        } else Result.failure(Exception("Failed: ${response.message()}"))
    } catch (e: Exception) { Result.failure(e) }
    
    override suspend fun cancelOrder(orderId: String) = try {
        val response = apiService.cancelOrder(orderId)
        if (response.isSuccessful) Result.success(Unit)
        else Result.failure(Exception("Failed: ${response.message()}"))
    } catch (e: Exception) { Result.failure(e) }
    
    override suspend fun refreshOrderStatus(orderId: String) = getOrderById(orderId)
}
