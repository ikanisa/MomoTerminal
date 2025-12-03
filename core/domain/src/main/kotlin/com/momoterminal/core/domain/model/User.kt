package com.momoterminal.core.domain.model

data class User(
    val id: String,
    val phone: String,
    val name: String? = null,
    val email: String? = null,
    val role: UserRole = UserRole.USER,
    val isVerified: Boolean = false
)

enum class UserRole {
    USER, MERCHANT, ADMIN
}
