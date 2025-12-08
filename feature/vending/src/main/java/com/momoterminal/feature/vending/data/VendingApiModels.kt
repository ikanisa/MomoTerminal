package com.momoterminal.feature.vending.data

import com.google.gson.annotations.SerializedName

data class VendingMachineDto(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("distance_meters") val distanceMeters: Int? = null,
    val status: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("product_size_ml") val productSizeML: Int = 500,
    val price: Long,
    val currency: String = "XAF",
    @SerializedName("stock_level") val stockLevel: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class VendingOrderDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("machine_id") val machineId: String,
    @SerializedName("machine_name") val machineName: String,
    @SerializedName("machine_location") val machineLocation: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("product_size_ml") val productSizeML: Int,
    val amount: Long,
    val status: String,
    @SerializedName("created_at") val createdAt: Long,
    val code: String? = null,
    @SerializedName("code_expires_at") val codeExpiresAt: Long? = null,
    @SerializedName("code_used_at") val codeUsedAt: Long? = null
)

data class CreateOrderRequest(
    @SerializedName("machine_id") val machineId: String,
    val amount: Long
)

data class CreateOrderResponse(
    val order: VendingOrderDto,
    val code: String,
    @SerializedName("code_expires_at") val codeExpiresAt: Long
)

data class MachinesResponse(
    val machines: List<VendingMachineDto>
)

data class OrdersResponse(
    val orders: List<VendingOrderDto>
)
