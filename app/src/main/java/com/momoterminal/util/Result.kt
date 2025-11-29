package com.momoterminal.util

/**
 * Sealed class representing the result of an operation.
 * Provides a clean way to handle success, error, and loading states.
 */
sealed class Result<out T> {
    
    /**
     * Represents a successful operation with data.
     */
    data class Success<out T>(val data: T) : Result<T>()
    
    /**
     * Represents an error with an exception.
     */
    data class Error(val exception: Throwable) : Result<Nothing>()
    
    /**
     * Represents a loading state.
     */
    data object Loading : Result<Nothing>()
    
    /**
     * Check if the result is successful.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if the result is an error.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Check if the result is loading.
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Get the data if successful, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Get the data if successful, or throw the exception.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }
    
    /**
     * Get the data if successful, or return the default value.
     */
    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }
    
    /**
     * Map the success value to a new type.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
        is Loading -> Loading
    }
    
    /**
     * Flat map to a new Result.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> Error(exception)
        is Loading -> Loading
    }
    
    /**
     * Execute action on success.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * Execute action on error.
     */
    inline fun onError(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) {
            action(exception)
        }
        return this
    }
    
    /**
     * Execute action on loading.
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) {
            action()
        }
        return this
    }
    
    companion object {
        /**
         * Create a Result from a nullable value.
         */
        fun <T> fromNullable(value: T?, error: () -> Throwable): Result<T> {
            return if (value != null) Success(value) else Error(error())
        }
        
        /**
         * Run a block and wrap the result.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Error(e)
            }
        }
    }
}
