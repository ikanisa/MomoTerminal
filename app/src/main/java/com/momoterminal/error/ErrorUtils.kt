package com.momoterminal.error

/**
 * Utility functions for error handling.
 */

/**
 * Get a user-friendly title for an error.
 */
fun AppError.getTitle(): String {
    return when (this) {
        is AppError.Network.NoConnection -> "No Connection"
        is AppError.Network.Timeout -> "Request Timed Out"
        is AppError.Network.ServerError -> "Server Error"
        is AppError.Network.Unknown -> "Network Error"
        is AppError.Api.Unauthorized -> "Authentication Required"
        is AppError.Api.Forbidden -> "Access Denied"
        is AppError.Api.NotFound -> "Not Found"
        is AppError.Api.BadRequest -> "Invalid Request"
        is AppError.Api.Conflict -> "Conflict"
        is AppError.Api.RateLimited -> "Too Many Requests"
        is AppError.Nfc.NotSupported -> "NFC Not Supported"
        is AppError.Nfc.Disabled -> "NFC Disabled"
        is AppError.Nfc.CommunicationFailed -> "NFC Error"
        is AppError.Nfc.Timeout -> "NFC Timeout"
        is AppError.Sms.PermissionDenied -> "SMS Permission Required"
        is AppError.Sms.ParseFailed -> "SMS Error"
        is AppError.Biometric.NotAvailable -> "Biometric Not Available"
        is AppError.Biometric.NotEnrolled -> "Biometric Not Set Up"
        is AppError.Biometric.Failed -> "Authentication Failed"
        is AppError.Biometric.Cancelled -> "Authentication Cancelled"
        is AppError.Database.ReadFailed -> "Read Error"
        is AppError.Database.WriteFailed -> "Save Error"
        is AppError.Database.DeleteFailed -> "Delete Error"
        is AppError.Database.NotFound -> "Data Not Found"
        is AppError.Validation.InvalidInput -> "Invalid Input"
        is AppError.Validation.MissingField -> "Missing Information"
        is AppError.Validation.InvalidFormat -> "Invalid Format"
        is AppError.Security.EncryptionFailed -> "Encryption Error"
        is AppError.Security.DecryptionFailed -> "Decryption Error"
        is AppError.Security.KeystoreError -> "Security Error"
        is AppError.Unknown -> "Something Went Wrong"
    }
}

/**
 * Get a category-level title for an error.
 */
fun AppError.getCategoryTitle(): String {
    return when (this) {
        is AppError.Network -> "Network Error"
        is AppError.Api -> "Server Error"
        is AppError.Nfc -> "NFC Error"
        is AppError.Sms -> "SMS Error"
        is AppError.Biometric -> "Authentication Error"
        is AppError.Database -> "Storage Error"
        is AppError.Validation -> "Validation Error"
        is AppError.Security -> "Security Error"
        is AppError.Unknown -> "Error"
    }
}
