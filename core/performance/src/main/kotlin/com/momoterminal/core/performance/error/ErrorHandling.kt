package com.momoterminal.core.performance.error

import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Generic error model for application errors.
 */
sealed class AppError {
    data class Network(
        val code: Int? = null,
        val message: String = "Network error"
    ) : AppError()
    
    data class Timeout(
        val message: String = "Request timed out"
    ) : AppError()
    
    data class Validation(
        val field: String,
        val message: String
    ) : AppError()
    
    data class NotFound(
        val resource: String = "Resource"
    ) : AppError()
    
    data class Unauthorized(
        val message: String = "Unauthorized"
    ) : AppError()
    
    data class ServerError(
        val code: Int = 500,
        val message: String = "Server error"
    ) : AppError()
    
    data class Unknown(
        val throwable: Throwable,
        val message: String = "An error occurred"
    ) : AppError()
    
    companion object {
        fun from(throwable: Throwable): AppError = when (throwable) {
            is SocketTimeoutException -> Timeout()
            is IOException -> Network(message = "Connection failed")
            else -> Unknown(throwable)
        }
    }
    
    fun toUserMessage(): String = when (this) {
        is Network -> "Connection issue. Please check your internet."
        is Timeout -> "Request took too long. Please try again."
        is Validation -> message
        is NotFound -> "$resource not found"
        is Unauthorized -> "Please log in again"
        is ServerError -> "Server error. Please try again later."
        is Unknown -> message
    }
    
    fun isRetryable(): Boolean = when (this) {
        is Network, is Timeout, is ServerError -> true
        else -> false
    }
}

/**
 * Retry policy for failed operations.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val multiplier: Double = 2.0
)
