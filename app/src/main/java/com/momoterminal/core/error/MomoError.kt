package com.momoterminal.core.error

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * Unified error model for MomoTerminal - designed for offline-first, low-connectivity environments.
 */
sealed class MomoError(
    open val message: String,
    open val cause: Throwable? = null
) {
    // Network errors
    data class NetworkUnavailable(override val message: String = "No internet connection") : MomoError(message)
    data class ServerError(val code: Int, override val message: String) : MomoError(message)
    data class Timeout(override val message: String = "Request timed out") : MomoError(message)

    // Permission errors
    data class PermissionDenied(val permission: String) : MomoError("Permission required: $permission")

    // NFC errors
    data class NfcDisabled(override val message: String = "NFC is disabled") : MomoError(message)
    data class NfcNotSupported(override val message: String = "Device doesn't support NFC") : MomoError(message)
    data class NfcReadError(override val cause: Throwable?) : MomoError("Failed to read NFC tag", cause)

    // SMS/Parsing errors
    data class SmsParseError(val rawSms: String, override val cause: Throwable?) : MomoError("Could not parse SMS", cause)

    // Database errors
    data class DatabaseError(override val message: String, override val cause: Throwable?) : MomoError(message, cause)

    // Sync errors
    data class SyncError(override val message: String, override val cause: Throwable?) : MomoError(message, cause)

    // Generic
    data class Unknown(override val cause: Throwable?) : MomoError(cause?.message ?: "Unknown error", cause)

    val isRecoverable: Boolean
        get() = when (this) {
            is NetworkUnavailable, is Timeout, is ServerError, is SyncError -> true
            else -> false
        }

    val requiresUserAction: Boolean
        get() = when (this) {
            is PermissionDenied, is NfcDisabled, is NfcNotSupported -> true
            else -> false
        }
}

fun Throwable.toMomoError(): MomoError = when (this) {
    is UnknownHostException, is ConnectException -> MomoError.NetworkUnavailable()
    is SocketTimeoutException -> MomoError.Timeout()
    is HttpException -> MomoError.ServerError(code(), message())
    else -> MomoError.Unknown(this)
}
