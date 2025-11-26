package com.momoterminal.security

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for biometric authentication.
 * Provides easy-to-use methods for fingerprint and face authentication.
 */
@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BiometricHelper"
    }
    
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * Result of biometric authentication.
     */
    sealed class BiometricResult {
        data object Success : BiometricResult()
        data class Error(val errorCode: Int, val errorMessage: String) : BiometricResult()
        data object Cancelled : BiometricResult()
        data object Failed : BiometricResult()
        data object NotAvailable : BiometricResult()
        data object NotEnrolled : BiometricResult()
        data object HardwareUnavailable : BiometricResult()
    }
    
    /**
     * Biometric availability status.
     */
    enum class BiometricStatus {
        AVAILABLE,
        NOT_AVAILABLE,
        NOT_ENROLLED,
        HARDWARE_UNAVAILABLE,
        SECURITY_UPDATE_REQUIRED,
        UNKNOWN
    }
    
    /**
     * Check if biometric authentication is available.
     */
    fun getBiometricStatus(): BiometricStatus {
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNKNOWN
        }
    }
    
    /**
     * Check if biometric authentication is available.
     */
    fun isBiometricAvailable(): Boolean {
        return getBiometricStatus() == BiometricStatus.AVAILABLE
    }
    
    /**
     * Check if device credential (PIN/Pattern/Password) is available.
     */
    fun isDeviceCredentialAvailable(): Boolean {
        return biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Check if any authentication method is available.
     */
    fun isAnyAuthenticationAvailable(): Boolean {
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Authenticate with biometrics.
     * 
     * @param activity The FragmentActivity to show the prompt in
     * @param title The title of the prompt
     * @param subtitle Optional subtitle
     * @param description Optional description
     * @param negativeButtonText Text for the negative button (cancel)
     * @param allowDeviceCredential Whether to allow PIN/Pattern/Password as fallback
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        allowDeviceCredential: Boolean = true
    ): Flow<BiometricResult> = callbackFlow {
        val status = getBiometricStatus()
        
        // Check availability
        if (status != BiometricStatus.AVAILABLE && !allowDeviceCredential) {
            trySend(when (status) {
                BiometricStatus.NOT_ENROLLED -> BiometricResult.NotEnrolled
                BiometricStatus.HARDWARE_UNAVAILABLE -> BiometricResult.HardwareUnavailable
                else -> BiometricResult.NotAvailable
            })
            close()
            return@callbackFlow
        }
        
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication succeeded")
                trySend(BiometricResult.Success)
                close()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "Authentication error: $errorCode - $errString")
                
                val result = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricResult.Cancelled
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricResult.NotEnrolled
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricResult.HardwareUnavailable
                    else -> BiometricResult.Error(errorCode, errString.toString())
                }
                trySend(result)
                close()
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "Authentication failed (bad biometric)")
                // Don't close - let user retry
                trySend(BiometricResult.Failed)
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .also { builder ->
                subtitle?.let { builder.setSubtitle(it) }
                description?.let { builder.setDescription(it) }
            }
        
        if (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use recommended setAllowedAuthenticators on Android 11+
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else if (allowDeviceCredential) {
            // Fallback to deprecated method for Android 10 and below
            // This is necessary for backward compatibility
            @Suppress("DEPRECATION")
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else {
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
        }
        
        val promptInfo = promptInfoBuilder.build()
        
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing biometric prompt", e)
            trySend(BiometricResult.Error(-1, e.message ?: "Unknown error"))
            close()
        }
        
        awaitClose {
            // Cleanup if needed
        }
    }
    
    /**
     * Authenticate for transaction confirmation.
     * Uses a preset title and description for payment confirmations.
     * 
     * @param activity The FragmentActivity
     * @param amount The payment amount
     * @param recipient The payment recipient
     */
    fun authenticateForPayment(
        activity: FragmentActivity,
        amount: String,
        recipient: String
    ): Flow<BiometricResult> {
        return authenticate(
            activity = activity,
            title = "Confirm Payment",
            subtitle = "GHS $amount to $recipient",
            description = "Use your fingerprint or face to confirm this payment",
            negativeButtonText = "Cancel",
            allowDeviceCredential = true
        )
    }
    
    /**
     * Get human-readable message for biometric status.
     */
    fun getStatusMessage(): String {
        return when (getBiometricStatus()) {
            BiometricStatus.AVAILABLE -> "Biometric authentication is available"
            BiometricStatus.NOT_AVAILABLE -> "This device doesn't support biometric authentication"
            BiometricStatus.NOT_ENROLLED -> "No biometrics enrolled. Please set up fingerprint or face in Settings"
            BiometricStatus.HARDWARE_UNAVAILABLE -> "Biometric hardware is currently unavailable"
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Security update required for biometric authentication"
            BiometricStatus.UNKNOWN -> "Unable to determine biometric status"
        }
    }
}
