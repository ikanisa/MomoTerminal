package com.momoterminal.feature.vending.data

import kotlinx.serialization.Serializable

@Serializable
data class VendingMachineDto(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val distance_meters: Int? = null,
    val status: String,
    val product_id: String,
    val product_name: String,
    val product_size_ml: Int = 500,
    val price: Long,
    val stock_level: String,
    val image_url: String? = null
)

@Serializable
data class VendingOrderDto(
    val id: String,
    val user_id: String,
    val machine_id: String,
    val machine_name: String,
    val machine_location: String,
    val product_name: String,
    val product_size_ml: Int,
    val amount: Long,
    val status: String,
    val created_at: Long,
    val code: String? = null,
    val code_expires_at: Long? = null,
    val code_used_at: Long? = null
)

@Serializable
data class CreateOrderRequest(
    val machine_id: String,
    val amount: Long
)

@Serializable
data class CreateOrderResponse(
    val order: VendingOrderDto,
    val code: String,
    val code_expires_at: Long
)

@Serializable
data class MachinesResponse(
    val machines: List<VendingMachineDto>
)

@Serializable
data class OrdersResponse(
    val orders: List<VendingOrderDto>
)
