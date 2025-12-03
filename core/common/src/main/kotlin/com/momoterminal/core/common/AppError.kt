package com.momoterminal.core.common

sealed class AppError : Exception() {
    data class Network(override val message: String = "Network error") : AppError()
    data class Auth(override val message: String = "Authentication failed") : AppError()
    data class Validation(override val message: String = "Validation error") : AppError()
    data class NotFound(override val message: String = "Resource not found") : AppError()
    data class Server(override val message: String = "Server error", val code: Int? = null) : AppError()
    data class Unknown(override val message: String = "Unknown error", override val cause: Throwable? = null) : AppError()
}
