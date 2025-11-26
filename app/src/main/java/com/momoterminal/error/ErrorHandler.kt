package com.momoterminal.error

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class for handling and converting exceptions to AppError.
 * Also manages error event emission for UI display.
 */
@Singleton
class ErrorHandler @Inject constructor() {
    
    companion object {
        private const val TAG = "ErrorHandler"
    }
    
    private val _errorEvents = MutableSharedFlow<ErrorEvent>(extraBufferCapacity = 10)
    val errorEvents: SharedFlow<ErrorEvent> = _errorEvents.asSharedFlow()
    
    /**
     * Convert a throwable to an AppError.
     */
    fun toAppError(throwable: Throwable): AppError {
        Log.e(TAG, "Error occurred: ${throwable.message}", throwable)
        
        return when (throwable) {
            // Network errors
            is UnknownHostException -> AppError.Network.NoConnection(throwable)
            is SocketTimeoutException -> AppError.Network.Timeout(throwable)
            is IOException -> AppError.Network.Unknown(
                message = throwable.message ?: "Network error occurred",
                cause = throwable
            )
            
            // HTTP errors
            is HttpException -> handleHttpException(throwable)
            
            // Already an AppError wrapped in a RuntimeException
            is RuntimeException -> {
                throwable.cause?.let { toAppError(it) } 
                    ?: AppError.Unknown(throwable.message ?: "Unknown error", throwable)
            }
            
            else -> AppError.Unknown(
                message = throwable.message ?: "An unexpected error occurred",
                cause = throwable
            )
        }
    }
    
    /**
     * Handle HTTP exceptions and convert to appropriate AppError.
     */
    private fun handleHttpException(exception: HttpException): AppError {
        return when (exception.code()) {
            400 -> AppError.Api.BadRequest(cause = exception)
            401 -> AppError.Api.Unauthorized(cause = exception)
            403 -> AppError.Api.Forbidden(cause = exception)
            404 -> AppError.Api.NotFound(cause = exception)
            409 -> AppError.Api.Conflict(cause = exception)
            429 -> {
                val retryAfter = exception.response()?.headers()?.get("Retry-After")?.toIntOrNull()
                AppError.Api.RateLimited(retryAfter, exception)
            }
            in 500..599 -> AppError.Network.ServerError(exception.code(), exception)
            else -> AppError.Network.Unknown(
                message = "HTTP error ${exception.code()}",
                cause = exception
            )
        }
    }
    
    /**
     * Emit an error event for display.
     */
    suspend fun emitError(error: AppError, retryAction: (() -> Unit)? = null) {
        val event = ErrorEvent.from(error, retryAction)
        _errorEvents.emit(event)
    }
    
    /**
     * Emit an error event from a throwable.
     */
    suspend fun emitError(throwable: Throwable, retryAction: (() -> Unit)? = null) {
        val error = toAppError(throwable)
        emitError(error, retryAction)
    }
    
    /**
     * Try to emit an error (non-suspending version).
     */
    fun tryEmitError(error: AppError, retryAction: (() -> Unit)? = null) {
        val event = ErrorEvent.from(error, retryAction)
        _errorEvents.tryEmit(event)
    }
    
    /**
     * Try to emit an error from a throwable (non-suspending version).
     */
    fun tryEmitError(throwable: Throwable, retryAction: (() -> Unit)? = null) {
        val error = toAppError(throwable)
        tryEmitError(error, retryAction)
    }
    
    /**
     * Execute a suspending block and handle any errors.
     */
    suspend fun <T> handleErrors(
        retryAction: (() -> Unit)? = null,
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            val error = toAppError(e)
            emitError(error, retryAction)
            Result.failure(e)
        }
    }
}
