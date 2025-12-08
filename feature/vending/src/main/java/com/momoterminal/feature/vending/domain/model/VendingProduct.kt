package com.momoterminal.feature.vending.domain.model

data class VendingProduct(
    val id: String,
    val name: String,
    val sizeML: Int,
    val price: Long,
    val description: String? = null,
    val imageUrl: String? = null
)
