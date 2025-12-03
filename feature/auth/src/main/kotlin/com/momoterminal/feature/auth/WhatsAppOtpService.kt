package com.momoterminal.feature.auth

import com.momoterminal.core.network.api.OtpSendResponse
import com.momoterminal.core.network.api.SendOtpRequest
import com.momoterminal.core.network.api.VerifyOtpRequest
import com.momoterminal.core.network.supabase.AuthResult
import com.momoterminal.core.network.supabase.SessionData
import com.momoterminal.core.network.supabase.SupabaseAuthService
import com.momoterminal.core.common.PhoneNumberValidator
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service interface for WhatsApp OTP operations.
 * Handles sending and verifying OTP codes via WhatsApp using Supabase edge functions.
 */
interface WhatsAppOtpService {
    /**
     * Send OTP to the specified phone number via WhatsApp.
     *
     * @param phoneNumber Phone number to send OTP to (will be formatted to E.164)
     * @return Result containing OtpSendResponse on success or error message
     */
    suspend fun sendOtp(phoneNumber: String): OtpResult<OtpSendResponse>
    
    /**
     * Verify the OTP code for the specified phone number.
     *
     * @param phoneNumber Phone number to verify
     * @param code The 6-digit OTP code
     * @return Result containing SessionData on success or error message
     */
    suspend fun verifyOtp(phoneNumber: String, code: String): OtpResult<SessionData>
    
    /**
     * Format phone number to WhatsApp-compatible E.164 format (+250XXXXXXXXX).
     *
     * @param phoneNumber Raw phone number input
     * @return Formatted phone number or null if invalid
     */
    fun formatPhoneNumber(phoneNumber: String): String?
}

/**
 * Result wrapper for OTP operations.
 */
sealed class OtpResult<out T> {
    data class Success<T>(val data: T) : OtpResult<T>()
    data class Error(
        val message: String,
        val code: OtpErrorCode = OtpErrorCode.UNKNOWN,
        val retryAfterSeconds: Int? = null
    ) : OtpResult<Nothing>()
}

/**
 * Error codes for OTP operations.
 */
enum class OtpErrorCode {
    INVALID_PHONE_NUMBER,
    RATE_LIMITED,
    OTP_EXPIRED,
    INVALID_OTP,
    MAX_ATTEMPTS_EXCEEDED,
    NETWORK_ERROR,
    SERVER_ERROR,
    UNKNOWN
}

/**
 * Implementation of WhatsAppOtpService using Supabase.
 * Uses the "momo_terminal" template for WhatsApp messages.
 */
@Singleton
class WhatsAppOtpServiceImpl @Inject constructor(
    private val supabaseAuthService: SupabaseAuthService,
    private val phoneNumberValidator: PhoneNumberValidator
) : WhatsAppOtpService {
    
    companion object {
        private const val WHATSAPP_CHANNEL = "whatsapp"
        private const val TEMPLATE_NAME = "momo_terminal"
        private const val OTP_EXPIRY_SECONDS = 300 // 5 minutes
        private const val OTP_CODE_LENGTH = 6
    }
    
    override suspend fun sendOtp(phoneNumber: String): OtpResult<OtpSendResponse> {
        Timber.d("Sending WhatsApp OTP to: $phoneNumber")
        
        // Validate and format phone number
        val formattedNumber = formatPhoneNumber(phoneNumber)
            ?: return OtpResult.Error(
                message = "Invalid phone number format",
                code = OtpErrorCode.INVALID_PHONE_NUMBER
            )
        
        return try {
            // Use Supabase auth service to send OTP
            when (val result = supabaseAuthService.sendWhatsAppOtp(formattedNumber)) {
                is AuthResult.Success -> {
                    Timber.d("WhatsApp OTP sent successfully to $formattedNumber")
                    OtpResult.Success(
                        OtpSendResponse(
                            success = true,
                            expiresInSeconds = OTP_EXPIRY_SECONDS
                        )
                    )
                }
                is AuthResult.Error -> {
                    Timber.e("Failed to send WhatsApp OTP: ${result.message}")
                    val errorCode = mapErrorCode(result.code)
                    OtpResult.Error(
                        message = result.message,
                        code = errorCode,
                        retryAfterSeconds = if (errorCode == OtpErrorCode.RATE_LIMITED) 60 else null
                    )
                }
                is AuthResult.Loading -> {
                    // This shouldn't happen as it's a suspend function
                    OtpResult.Error(
                        message = "Unexpected loading state",
                        code = OtpErrorCode.UNKNOWN
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while sending WhatsApp OTP")
            OtpResult.Error(
                message = e.message ?: "Failed to send OTP",
                code = OtpErrorCode.NETWORK_ERROR
            )
        }
    }
    
    override suspend fun verifyOtp(phoneNumber: String, code: String): OtpResult<SessionData> {
        Timber.d("Verifying OTP for: $phoneNumber")
        
        // Validate phone number
        val formattedNumber = formatPhoneNumber(phoneNumber)
            ?: return OtpResult.Error(
                message = "Invalid phone number format",
                code = OtpErrorCode.INVALID_PHONE_NUMBER
            )
        
        // Validate OTP code format
        if (!isValidOtpFormat(code)) {
            return OtpResult.Error(
                message = "OTP must be $OTP_CODE_LENGTH digits",
                code = OtpErrorCode.INVALID_OTP
            )
        }
        
        return try {
            when (val result = supabaseAuthService.verifyOtp(formattedNumber, code)) {
                is AuthResult.Success -> {
                    Timber.d("OTP verified successfully for $formattedNumber")
                    OtpResult.Success(result.data)
                }
                is AuthResult.Error -> {
                    Timber.e("OTP verification failed: ${result.message}")
                    val errorCode = mapVerificationErrorCode(result.code)
                    OtpResult.Error(
                        message = result.message,
                        code = errorCode
                    )
                }
                is AuthResult.Loading -> {
                    OtpResult.Error(
                        message = "Unexpected loading state",
                        code = OtpErrorCode.UNKNOWN
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception while verifying OTP")
            OtpResult.Error(
                message = e.message ?: "Failed to verify OTP",
                code = OtpErrorCode.NETWORK_ERROR
            )
        }
    }
    
    override fun formatPhoneNumber(phoneNumber: String): String? {
        val result = phoneNumberValidator.validate(phoneNumber)
        return if (result.isValid) {
            result.formattedNumber
        } else {
            Timber.w("Invalid phone number: ${result.errorMessage}")
            null
        }
    }
    
    /**
     * Validate OTP code format.
     */
    private fun isValidOtpFormat(code: String): Boolean {
        return code.length == OTP_CODE_LENGTH && code.all { it.isDigit() }
    }
    
    /**
     * Map Supabase error codes to OtpErrorCode.
     */
    private fun mapErrorCode(code: String?): OtpErrorCode {
        return when (code) {
            "rate_limit_exceeded", "too_many_requests" -> OtpErrorCode.RATE_LIMITED
            "invalid_phone" -> OtpErrorCode.INVALID_PHONE_NUMBER
            "server_error", "internal_error" -> OtpErrorCode.SERVER_ERROR
            else -> OtpErrorCode.UNKNOWN
        }
    }
    
    /**
     * Map verification error codes to OtpErrorCode.
     */
    private fun mapVerificationErrorCode(code: String?): OtpErrorCode {
        return when (code) {
            "otp_expired", "token_expired" -> OtpErrorCode.OTP_EXPIRED
            "invalid_token", "OTP_VERIFICATION_FAILED" -> OtpErrorCode.INVALID_OTP
            "max_attempts", "too_many_attempts" -> OtpErrorCode.MAX_ATTEMPTS_EXCEEDED
            "rate_limit_exceeded" -> OtpErrorCode.RATE_LIMITED
            else -> OtpErrorCode.UNKNOWN
        }
    }
}
