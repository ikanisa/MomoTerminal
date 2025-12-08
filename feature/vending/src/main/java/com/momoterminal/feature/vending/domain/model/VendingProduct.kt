package com.momoterminal.feature.vending.domain.model

data class VendingProduct(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val servingSizeML: Int = 500,
    val pricePerServing: Long,
    val description: String? = null,
    val imageUrl: String? = null,
    val isAgeRestricted: Boolean = false
) {
    fun isAlcoholic(): Boolean = category == ProductCategory.ALCOHOL || category == ProductCategory.BEER
}

enum class ProductCategory(val displayName: String, val requiresAgeVerification: Boolean) {
    JUICE("Juice", false),
    HOT_COFFEE("Hot Coffee", false),
    COCKTAIL("Cocktail", false),
    ALCOHOL("Alcohol", true),
    BEER("Beer", true)
}
