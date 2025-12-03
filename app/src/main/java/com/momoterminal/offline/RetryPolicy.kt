package com.momoterminal.offline

import com.momoterminal.core.logging.MomoLogger
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Retry policy for network operations with exponential backoff.
 * Designed for low-connectivity environments.
 */
object RetryPolicy {
    
    private const val TAG = "RetryPolicy"
    
    /**
     * Execute a suspending operation with retry logic.
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 30000,
        factor: Double = 2.0,
        retryOn: (Throwable) -> Boolean = { true },
        operation: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastException: Throwable? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return Result.success(operation())
            } catch (e: Throwable) {
                lastException = e
                
                if (!retryOn(e)) {
                    MomoLogger.w(TAG, "Non-retryable error: ${e.message}")
                    return Result.failure(e)
                }
                
                if (attempt < maxAttempts - 1) {
                    MomoLogger.d(TAG, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
                    delay(currentDelay)
                    currentDelay = min((currentDelay * factor).toLong(), maxDelayMs)
                }
            }
        }
        
        MomoLogger.e(TAG, "All $maxAttempts attempts failed", lastException)
        return Result.failure(lastException ?: Exception("Unknown error"))
    }
    
    /**
     * Execute with retry, returning null on failure.
     */
    suspend fun <T> withRetryOrNull(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        operation: suspend () -> T
    ): T? {
        return withRetry(maxAttempts, initialDelayMs, operation = operation).getOrNull()
    }
    
    /**
     * Check if an exception is retryable.
     */
    fun isRetryable(e: Throwable): Boolean {
        return when (e) {
            is java.net.SocketTimeoutException,
            is java.net.ConnectException,
            is java.net.UnknownHostException,
            is java.io.IOException -> true
            is retrofit2.HttpException -> e.code() in listOf(408, 429, 500, 502, 503, 504)
            else -> false
        }
    }
}

/**
 * Extension function for easy retry.
 */
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    operation: suspend () -> T
): Result<T> = RetryPolicy.withRetry(
    maxAttempts = maxAttempts,
    retryOn = RetryPolicy::isRetryable,
    operation = operation
)
