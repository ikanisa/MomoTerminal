package com.momoterminal.error

/**
 * Sealed class hierarchy representing all possible application errors.
 * Provides a type-safe way to handle different error scenarios.
 */
sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    
    // ============= Network Errors =============
    
    sealed class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class NoConnection(
            override val cause: Throwable? = null
        ) : Network("No internet connection. Please check your network settings.", cause)
        
        data class Timeout(
            override val cause: Throwable? = null
        ) : Network("Request timed out. Please try again.", cause)
        
        data class ServerError(
            val code: Int,
            override val cause: Throwable? = null
        ) : Network("Server error ($code). Please try again later.", cause)
        
        data class Unknown(
            override val message: String = "A network error occurred.",
            override val cause: Throwable? = null
        ) : Network(message, cause)
    }
    
    // ============= API Errors =============
    
    sealed class Api(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class Unauthorized(
            override val cause: Throwable? = null
        ) : Api("Authentication required. Please log in again.", cause)
        
        data class Forbidden(
            override val cause: Throwable? = null
        ) : Api("You don't have permission to perform this action.", cause)
        
        data class NotFound(
            override val cause: Throwable? = null
        ) : Api("The requested resource was not found.", cause)
        
        data class BadRequest(
            override val message: String = "Invalid request. Please check your input.",
            override val cause: Throwable? = null
        ) : Api(message, cause)
        
        data class Conflict(
            override val cause: Throwable? = null
        ) : Api("A conflict occurred. The data may have been modified.", cause)
        
        data class RateLimited(
            val retryAfterSeconds: Int? = null,
            override val cause: Throwable? = null
        ) : Api("Too many requests. Please wait before trying again.", cause)
    }
    
    // ============= NFC Errors =============
    
    sealed class Nfc(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data object NotSupported : Nfc("NFC is not supported on this device.")
        
        data object Disabled : Nfc("NFC is disabled. Please enable it in settings.")
        
        data class CommunicationFailed(
            override val cause: Throwable? = null
        ) : Nfc("Failed to communicate with the NFC device.", cause)
        
        data class Timeout(
            override val cause: Throwable? = null
        ) : Nfc("NFC communication timed out. Please try again.", cause)
    }
    
    // ============= SMS Errors =============
    
    sealed class Sms(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data object PermissionDenied : Sms("SMS permission is required to receive payments.")
        
        data class ParseFailed(
            override val cause: Throwable? = null
        ) : Sms("Failed to parse the SMS message.", cause)
    }
    
    // ============= Biometric Errors =============
    
    sealed class Biometric(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data object NotAvailable : Biometric("Biometric authentication is not available on this device.")
        
        data object NotEnrolled : Biometric("No biometric credentials enrolled. Please set up fingerprint or face recognition.")
        
        data class Failed(
            override val cause: Throwable? = null
        ) : Biometric("Biometric authentication failed.", cause)
        
        data object Cancelled : Biometric("Authentication cancelled.")
    }
    
    // ============= Database Errors =============
    
    sealed class Database(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class ReadFailed(
            override val cause: Throwable? = null
        ) : Database("Failed to read data from storage.", cause)
        
        data class WriteFailed(
            override val cause: Throwable? = null
        ) : Database("Failed to save data to storage.", cause)
        
        data class DeleteFailed(
            override val cause: Throwable? = null
        ) : Database("Failed to delete data.", cause)
        
        data class NotFound(
            override val cause: Throwable? = null
        ) : Database("The requested data was not found.", cause)
    }
    
    // ============= Validation Errors =============
    
    sealed class Validation(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class InvalidInput(
            val field: String,
            override val message: String = "Invalid $field"
        ) : Validation(message)
        
        data class MissingField(
            val field: String
        ) : Validation("$field is required")
        
        data class InvalidFormat(
            val field: String,
            val expectedFormat: String
        ) : Validation("$field has an invalid format. Expected: $expectedFormat")
    }
    
    // ============= Security Errors =============
    
    sealed class Security(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {
        
        data class EncryptionFailed(
            override val cause: Throwable? = null
        ) : Security("Failed to encrypt data.", cause)
        
        data class DecryptionFailed(
            override val cause: Throwable? = null
        ) : Security("Failed to decrypt data.", cause)
        
        data object KeystoreError : Security("Secure storage is not available.")
    }
    
    // ============= Unknown/Generic Errors =============
    
    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
    
    /**
     * Check if this error is recoverable (can be retried).
     */
    val isRecoverable: Boolean
        get() = when (this) {
            is Network.NoConnection,
            is Network.Timeout,
            is Network.ServerError,
            is Api.RateLimited,
            is Nfc.CommunicationFailed,
            is Nfc.Timeout,
            is Database.ReadFailed,
            is Database.WriteFailed -> true
            else -> false
        }
    
    /**
     * Check if this error requires user action (e.g., enabling NFC).
     */
    val requiresUserAction: Boolean
        get() = when (this) {
            is Nfc.Disabled,
            is Nfc.NotSupported,
            is Sms.PermissionDenied,
            is Biometric.NotEnrolled,
            is Biometric.NotAvailable -> true
            else -> false
        }
}
