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
    @SerializedName("product_category") val productCategory: String,
    @SerializedName("serving_size_ml") val servingSizeML: Int = 500,
    @SerializedName("price_per_serving") val pricePerServing: Long,
    val currency: String = "XAF",
    @SerializedName("stock_level") val stockLevel: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("is_age_restricted") val isAgeRestricted: Boolean = false
)

data class VendingOrderDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("machine_id") val machineId: String,
    @SerializedName("machine_name") val machineName: String,
    @SerializedName("machine_location") val machineLocation: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("product_category") val productCategory: String,
    val quantity: Int,
    @SerializedName("serving_size_ml") val servingSizeML: Int,
    @SerializedName("price_per_serving") val pricePerServing: Long,
    @SerializedName("total_amount") val totalAmount: Long,
    val status: String,
    @SerializedName("created_at") val createdAt: Long,
    val code: VendingCodeDto? = null
)

data class VendingCodeDto(
    val code: String,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("total_serves") val totalServes: Int,
    @SerializedName("remaining_serves") val remainingServes: Int,
    @SerializedName("used_at") val usedAt: Long? = null,
    @SerializedName("closed_at") val closedAt: Long? = null
)

data class CreateOrderRequest(
    @SerializedName("machine_id") val machineId: String,
    val quantity: Int
)

data class CreateOrderResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("order_status") val orderStatus: String,
    val code: String,
    @SerializedName("code_expires_at") val codeExpiresAt: Long,
    @SerializedName("total_serves") val totalServes: Int,
    @SerializedName("remaining_serves") val remainingServes: Int,
    @SerializedName("wallet_balance") val walletBalance: Long
)

data class MachinesResponse(
    val machines: List<VendingMachineDto>
)

data class OrdersResponse(
    val orders: List<VendingOrderDto>
)

data class AgeVerificationDto(
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("date_of_birth") val dateOfBirth: String? = null,
    @SerializedName("verification_method") val verificationMethod: String? = null
)
