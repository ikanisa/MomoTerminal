package com.momoterminal.security

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager for Google Play Integrity API integration.
 * 
 * Play Integrity API verifies that the app and device are genuine and unmodified,
 * helping protect against:
 * - Tampered or modified app binaries
 * - Rooted or compromised devices
 * - Emulators and virtual environments
 * - Apps not installed from Play Store
 */
@Singleton
class PlayIntegrityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val integrityManager: IntegrityManager by lazy {
        IntegrityManagerFactory.create(context)
    }

    /**
     * Result of an integrity check operation.
     */
    sealed class IntegrityResult {
        /**
         * Integrity check passed successfully.
         * @param token The integrity token to send to your server for verification
         */
        data class Success(val token: String) : IntegrityResult()

        /**
         * Integrity check failed.
         * @param errorCode Error code from Play Integrity API
         * @param message Human-readable error message
         */
        data class Failure(
            val errorCode: Int,
            val message: String
        ) : IntegrityResult()

        /**
         * Integrity check could not be performed.
         * @param exception The exception that occurred
         */
        data class Error(val exception: Exception) : IntegrityResult()
    }

    /**
     * Requests an integrity token from the Play Integrity API.
     * 
     * The returned token should be sent to your server for verification.
     * Never verify the token on the client device.
     * 
     * @param nonce A unique, one-time use nonce for this request.
     *              Should be at least 16 characters and cryptographically random.
     * @return IntegrityResult indicating success, failure, or error
     */
    suspend fun requestIntegrityToken(nonce: String): IntegrityResult {
        return try {
            val token = requestTokenInternal(nonce)
            IntegrityResult.Success(token)
        } catch (e: Exception) {
            Timber.e(e, "Play Integrity check failed")
            when {
                e is PlayIntegrityException -> IntegrityResult.Failure(e.errorCode, e.message ?: "Unknown error")
                else -> IntegrityResult.Error(e)
            }
        }
    }

    /**
     * Internal function to request the integrity token.
     */
    private suspend fun requestTokenInternal(nonce: String): String {
        return suspendCancellableCoroutine { continuation ->
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    val token = response.token()
                    if (token.isNotEmpty()) {
                        continuation.resume(token)
                    } else {
                        continuation.resumeWithException(
                            PlayIntegrityException(ERROR_EMPTY_TOKEN, "Empty integrity token received")
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    val errorCode = extractErrorCode(exception)
                    val errorMessage = getErrorMessage(errorCode)
                    continuation.resumeWithException(
                        PlayIntegrityException(errorCode, errorMessage)
                    )
                }
        }
    }

    /**
     * Extracts error code from Play Integrity exception.
     */
    private fun extractErrorCode(exception: Exception): Int {
        // Try to extract the error code from the exception message or cause
        val message = exception.message ?: ""
        
        return when {
            message.contains("API_NOT_AVAILABLE") -> ERROR_API_NOT_AVAILABLE
            message.contains("APP_NOT_INSTALLED") -> ERROR_APP_NOT_INSTALLED
            message.contains("APP_UID_MISMATCH") -> ERROR_APP_UID_MISMATCH
            message.contains("CANNOT_BIND_TO_SERVICE") -> ERROR_CANNOT_BIND
            message.contains("CLOUD_PROJECT_NUMBER_IS_INVALID") -> ERROR_CLOUD_PROJECT_INVALID
            message.contains("GOOGLE_SERVER_UNAVAILABLE") -> ERROR_GOOGLE_SERVER_UNAVAILABLE
            message.contains("INTERNAL_ERROR") -> ERROR_INTERNAL
            message.contains("NETWORK_ERROR") -> ERROR_NETWORK
            message.contains("NO_ERROR") -> ERROR_NONE
            message.contains("NONCE_IS_NOT_BASE64") -> ERROR_NONCE_NOT_BASE64
            message.contains("NONCE_TOO_LONG") -> ERROR_NONCE_TOO_LONG
            message.contains("NONCE_TOO_SHORT") -> ERROR_NONCE_TOO_SHORT
            message.contains("PLAY_SERVICES_NOT_FOUND") -> ERROR_PLAY_SERVICES_NOT_FOUND
            message.contains("PLAY_SERVICES_VERSION_OUTDATED") -> ERROR_PLAY_SERVICES_OUTDATED
            message.contains("PLAY_STORE_NOT_FOUND") -> ERROR_PLAY_STORE_NOT_FOUND
            message.contains("TOO_MANY_REQUESTS") -> ERROR_TOO_MANY_REQUESTS
            else -> ERROR_UNKNOWN
        }
    }

    /**
     * Gets a human-readable error message for the error code.
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            ERROR_API_NOT_AVAILABLE -> "Play Integrity API is not available"
            ERROR_APP_NOT_INSTALLED -> "App is not installed from Play Store"
            ERROR_APP_UID_MISMATCH -> "App UID mismatch detected"
            ERROR_CANNOT_BIND -> "Cannot bind to Play Integrity service"
            ERROR_CLOUD_PROJECT_INVALID -> "Cloud project number is invalid"
            ERROR_GOOGLE_SERVER_UNAVAILABLE -> "Google servers are unavailable"
            ERROR_INTERNAL -> "Internal error occurred"
            ERROR_NETWORK -> "Network error occurred"
            ERROR_NONCE_NOT_BASE64 -> "Nonce is not valid Base64"
            ERROR_NONCE_TOO_LONG -> "Nonce is too long"
            ERROR_NONCE_TOO_SHORT -> "Nonce is too short (minimum 16 characters)"
            ERROR_PLAY_SERVICES_NOT_FOUND -> "Google Play Services not found"
            ERROR_PLAY_SERVICES_OUTDATED -> "Google Play Services is outdated"
            ERROR_PLAY_STORE_NOT_FOUND -> "Google Play Store not found"
            ERROR_TOO_MANY_REQUESTS -> "Too many integrity check requests"
            ERROR_EMPTY_TOKEN -> "Empty integrity token received"
            else -> "Unknown error occurred"
        }
    }

    /**
     * Generates a cryptographically secure nonce for integrity requests.
     * 
     * WARNING: This method is provided for testing/development purposes only.
     * In production, nonces should ALWAYS be generated server-side and sent 
     * to the client to prevent replay attacks and ensure freshness.
     * 
     * The nonce should be:
     * - At least 16 characters long
     * - Base64 encoded
     * - Unique per request
     * - Generated server-side in production (NOT client-side)
     * 
     * @return A Base64-encoded random nonce for development/testing
     */
    @Deprecated(
        message = "Nonces should be generated server-side in production. " +
                "This method is for development/testing only.",
        level = DeprecationLevel.WARNING
    )
    fun generateNonce(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }

    /**
     * Custom exception for Play Integrity errors.
     */
    class PlayIntegrityException(
        val errorCode: Int,
        override val message: String
    ) : Exception(message)

    companion object {
        // Error codes
        const val ERROR_NONE = 0
        const val ERROR_API_NOT_AVAILABLE = -1
        const val ERROR_APP_NOT_INSTALLED = -2
        const val ERROR_APP_UID_MISMATCH = -3
        const val ERROR_CANNOT_BIND = -4
        const val ERROR_CLOUD_PROJECT_INVALID = -5
        const val ERROR_GOOGLE_SERVER_UNAVAILABLE = -6
        const val ERROR_INTERNAL = -7
        const val ERROR_NETWORK = -8
        const val ERROR_NONCE_NOT_BASE64 = -9
        const val ERROR_NONCE_TOO_LONG = -10
        const val ERROR_NONCE_TOO_SHORT = -11
        const val ERROR_PLAY_SERVICES_NOT_FOUND = -12
        const val ERROR_PLAY_SERVICES_OUTDATED = -13
        const val ERROR_PLAY_STORE_NOT_FOUND = -14
        const val ERROR_TOO_MANY_REQUESTS = -15
        const val ERROR_EMPTY_TOKEN = -16
        const val ERROR_UNKNOWN = -999
    }
}
