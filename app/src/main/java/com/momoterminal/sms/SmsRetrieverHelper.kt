package com.momoterminal.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetrieverClient
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for SMS Retriever API integration.
 * Provides zero-permission SMS retrieval for app-specific messages.
 */
@Singleton
class SmsRetrieverHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SmsRetrieverHelper"
    }
    
    private val smsRetrieverClient: SmsRetrieverClient = SmsRetriever.getClient(context)
    
    /**
     * Result of SMS retrieval.
     */
    sealed class SmsResult {
        data class Success(val message: String) : SmsResult()
        data class Error(val message: String, val statusCode: Int? = null) : SmsResult()
        data object Timeout : SmsResult()
    }
    
    /**
     * Start the SMS Retriever API to listen for incoming SMS.
     * The API will listen for 5 minutes for a matching SMS.
     * 
     * @return true if successfully started, false otherwise
     */
    fun startSmsRetriever(): Boolean {
        return try {
            val task = smsRetrieverClient.startSmsRetriever()
            task.addOnSuccessListener {
                Log.d(TAG, "SMS Retriever started successfully")
            }
            task.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start SMS Retriever", e)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SMS Retriever", e)
            false
        }
    }
    
    /**
     * Start SMS Retriever for user consent flow.
     * This shows the user a prompt to share the SMS.
     * 
     * @return true if successfully started, false otherwise
     */
    fun startSmsUserConsent(senderPhoneNumber: String? = null): Boolean {
        return try {
            val task = smsRetrieverClient.startSmsUserConsent(senderPhoneNumber)
            task.addOnSuccessListener {
                Log.d(TAG, "SMS User Consent started successfully")
            }
            task.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start SMS User Consent", e)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SMS User Consent", e)
            false
        }
    }
    
    /**
     * Get a Flow of SMS messages using SMS Retriever API.
     * Register a broadcast receiver and emit messages as they arrive.
     */
    fun getSmsFlow(): Flow<SmsResult> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    SmsRetriever.SMS_RETRIEVED_ACTION -> {
                        val extras = intent.extras
                        val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status
                        
                        when (status?.statusCode) {
                            CommonStatusCodes.SUCCESS -> {
                                val message = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                                if (message != null) {
                                    Log.d(TAG, "SMS received via Retriever")
                                    trySend(SmsResult.Success(message))
                                } else {
                                    trySend(SmsResult.Error("Empty message received"))
                                }
                            }
                            CommonStatusCodes.TIMEOUT -> {
                                Log.d(TAG, "SMS Retriever timed out")
                                trySend(SmsResult.Timeout)
                            }
                            else -> {
                                Log.e(TAG, "SMS Retriever error: ${status?.statusCode}")
                                trySend(SmsResult.Error(
                                    "Failed to retrieve SMS",
                                    status?.statusCode
                                ))
                            }
                        }
                    }
                }
            }
        }
        
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        
        // Start the retriever
        startSmsRetriever()
        
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering receiver", e)
            }
        }
    }
    
    /**
     * Generate app hash for SMS Retriever.
     * The hash is used to identify SMS messages meant for this app.
     * 
     * Note: This should be generated once and included in the SMS format.
     * Format: <#> Your verification code is: 123456
     *         <app_hash>
     */
    fun getAppSignatureHash(): String {
        return try {
            val helper = AppSignatureHelper(context)
            helper.getAppSignatures().firstOrNull() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app signature", e)
            ""
        }
    }
    
    /**
     * Parse the verification code from an SMS message.
     * Adjust the regex pattern based on your SMS format.
     */
    fun extractVerificationCode(message: String): String? {
        // Common patterns for verification codes
        val patterns = listOf(
            "\\b(\\d{4,8})\\b",  // 4-8 digit code
            "(?:code|Code|CODE)\\s*:?\\s*(\\d{4,8})",
            "(?:OTP|otp)\\s*:?\\s*(\\d{4,8})"
        )
        
        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(message)
            if (match != null) {
                return match.groupValues.getOrNull(1)
            }
        }
        
        return null
    }
}

/**
 * Helper class to get the app signature hash.
 * This is needed for SMS Retriever API to work.
 */
class AppSignatureHelper(private val context: Context) {
    companion object {
        private const val TAG = "AppSignatureHelper"
        private const val HASH_TYPE = "SHA-256"
        private const val NUM_HASHED_BYTES = 9
        private const val NUM_BASE64_CHAR = 11
    }
    
    fun getAppSignatures(): List<String> {
        val signatureList = mutableListOf<String>()
        
        try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            
            @Suppress("DEPRECATION")
            val signatures = packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            ).signatures
            
            for (signature in signatures.orEmpty()) {
                val hash = hash(packageName, signature.toCharsString())
                if (hash != null) {
                    signatureList.add(hash)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app signatures", e)
        }
        
        return signatureList
    }
    
    private fun hash(packageName: String, signature: String): String? {
        return try {
            val appInfo = "$packageName $signature"
            val messageDigest = java.security.MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray(Charsets.UTF_8))
            val hashSignature = messageDigest.digest()
            
            // Truncate to first 9 bytes and base64 encode
            val truncatedHash = hashSignature.copyOfRange(0, NUM_HASHED_BYTES)
            val base64Hash = android.util.Base64.encodeToString(
                truncatedHash,
                android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
            
            base64Hash.substring(0, NUM_BASE64_CHAR)
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing signature", e)
            null
        }
    }
}
