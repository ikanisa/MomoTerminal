package com.momoterminal.feature.vending.data

import retrofit2.Response
import retrofit2.http.*

interface VendingApiService {
    @GET("vending/machines")
    suspend fun getMachines(
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius_km") radiusKm: Int? = null
    ): Response<MachinesResponse>
    
    @GET("vending/machines/{id}")
    suspend fun getMachineById(@Path("id") machineId: String): Response<VendingMachineDto>
    
    @POST("vending/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>
    
    @GET("vending/orders")
    suspend fun getOrders(): Response<OrdersResponse>
    
    @GET("vending/orders/{id}")
    suspend fun getOrderById(@Path("id") orderId: String): Response<VendingOrderDto>
    
    @POST("vending/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") orderId: String): Response<Unit>
}
