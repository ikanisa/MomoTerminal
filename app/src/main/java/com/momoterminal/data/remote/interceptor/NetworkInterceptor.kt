package com.momoterminal.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

/**
 * OkHttp Interceptor for handling network-related concerns.
 * Adds retry logic and custom error handling.
 * 
 * Note: This interceptor uses Thread.sleep for backoff delays. While this blocks
 * the OkHttp dispatcher thread, OkHttp uses a thread pool with configurable size
 * (default 64 threads) which mitigates the impact. For high-throughput scenarios,
 * consider using OkHttp's built-in retry mechanisms or custom async handling.
 */
class NetworkInterceptor @Inject constructor() : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        var response: Response? = null
        var exception: IOException? = null
        
        // Retry logic with exponential backoff
        for (attempt in 0 until MAX_RETRIES) {
            try {
                response = chain.proceed(request)
                
                // If response is successful or client error (4xx), don't retry
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                
                // For server errors (5xx), close response and retry
                if (response.code in 500..599 && attempt < MAX_RETRIES - 1) {
                    response.close()
                    Thread.sleep(getBackoffDelay(attempt))
                    continue
                }
                
                return response
                
            } catch (e: IOException) {
                exception = e
                
                // Don't retry on last attempt
                if (attempt == MAX_RETRIES - 1) {
                    throw e
                }
                
                // Wait before retrying
                try {
                    Thread.sleep(getBackoffDelay(attempt))
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Request interrupted", ie)
                }
            }
        }
        
        // Should not reach here, but just in case
        exception?.let { throw it }
        return response ?: throw IOException("Unknown network error")
    }
    
    /**
     * Calculate exponential backoff delay.
     */
    private fun getBackoffDelay(attempt: Int): Long {
        val delay = INITIAL_BACKOFF_MS * (1 shl attempt)
        return minOf(delay, MAX_BACKOFF_MS)
    }
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 8000L
    }
}
