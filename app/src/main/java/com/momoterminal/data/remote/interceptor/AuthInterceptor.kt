package com.momoterminal.data.remote.interceptor

import com.momoterminal.security.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp Interceptor for adding authentication headers to requests.
 */
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get API token and secret from secure storage
        val apiToken = secureStorage.getApiToken()
        val apiSecret = secureStorage.getApiSecret()
        
        // Build new request with auth headers
        val newRequest = originalRequest.newBuilder().apply {
            // Add API key header if available
            apiSecret?.let { secret ->
                if (secret.isNotBlank()) {
                    addHeader(HEADER_API_KEY, secret)
                }
            }
            
            // Add Authorization header if token is available
            apiToken?.let { token ->
                if (token.isNotBlank()) {
                    addHeader(HEADER_AUTHORIZATION, "Bearer $token")
                }
            }
            
            // Add common headers
            addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
            addHeader(HEADER_ACCEPT, CONTENT_TYPE_JSON)
        }.build()
        
        return chain.proceed(newRequest)
    }
    
    companion object {
        private const val HEADER_API_KEY = "X-Api-Key"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private const val HEADER_ACCEPT = "Accept"
        private const val CONTENT_TYPE_JSON = "application/json"
    }
}
