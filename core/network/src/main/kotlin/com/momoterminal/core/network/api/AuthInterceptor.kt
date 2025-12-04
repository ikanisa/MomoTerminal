package com.momoterminal.core.network.api

import com.momoterminal.core.domain.repository.AuthRepository
import com.momoterminal.core.common.auth.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * OkHttp interceptor for adding authentication headers and handling token refresh.
 * 
 * Responsibilities:
 * - Add Authorization header to all requests
 * - Handle 401 responses with automatic token refresh
 * - Logout on refresh failure
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    // Use Provider to avoid circular dependency with AuthRepository
    private val authRepositoryProvider: Provider<AuthRepository>
) : Interceptor {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_BEARER = "Bearer"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        
        // Paths that don't require authentication
        private val EXCLUDED_PATHS = listOf(
            "auth/login",
            "auth/register",
            "auth/refresh",
            "auth/request-otp",
            "auth/verify-otp"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestPath = originalRequest.url.encodedPath

        // Skip authentication for excluded paths
        if (EXCLUDED_PATHS.any { requestPath.contains(it) }) {
            return chain.proceed(originalRequest)
        }

        // Add authorization header if token exists
        val accessToken = tokenManager.getAccessToken()
        val request = if (accessToken != null) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$HEADER_BEARER $accessToken")
                .build()
        } else {
            originalRequest
        }

        var response = chain.proceed(request)

        // Handle 401 Unauthorized - attempt token refresh
        if (response.code == HTTP_UNAUTHORIZED && accessToken != null) {
            Timber.d("Received 401, attempting token refresh")
            
            response.close()
            
            // Attempt token refresh
            val refreshSuccess = runBlocking {
                try {
                    authRepositoryProvider.get().refreshToken()
                } catch (e: Exception) {
                    Timber.e(e, "Token refresh failed")
                    false
                }
            }

            if (refreshSuccess) {
                // Retry original request with new token
                val newToken = tokenManager.getAccessToken()
                if (newToken != null) {
                    val newRequest = originalRequest.newBuilder()
                        .header(HEADER_AUTHORIZATION, "$HEADER_BEARER $newToken")
                        .build()
                    response = chain.proceed(newRequest)
                    Timber.d("Request retried with new token")
                }
            } else {
                // Refresh failed - logout user
                Timber.w("Token refresh failed, logging out")
                runBlocking {
                    authRepositoryProvider.get().logout()
                }
            }
        }

        // Handle 403 Forbidden - might indicate revoked token
        if (response.code == HTTP_FORBIDDEN) {
            Timber.w("Received 403 Forbidden")
            // Could add additional handling here if needed
        }

        return response
    }
}
