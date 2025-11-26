package com.momoterminal.webhook

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for generating HMAC-SHA256 signatures for webhook requests.
 */
@Singleton
class HmacSigner @Inject constructor() {
    
    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
    }
    
    /**
     * Generate an HMAC-SHA256 signature for the given payload.
     *
     * @param payload The payload to sign (typically the request body)
     * @param secret The secret key for signing
     * @return The Base64-encoded HMAC-SHA256 signature
     */
    fun sign(payload: String, secret: String): String {
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256)
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hmacBytes, Base64.NO_WRAP)
    }
    
    /**
     * Generate a hex-encoded HMAC-SHA256 signature.
     *
     * @param payload The payload to sign
     * @param secret The secret key for signing
     * @return The hex-encoded HMAC-SHA256 signature
     */
    fun signHex(payload: String, secret: String): String {
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256)
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verify an HMAC-SHA256 signature.
     *
     * @param payload The original payload
     * @param signature The signature to verify (Base64-encoded)
     * @param secret The secret key
     * @return True if the signature is valid
     */
    fun verify(payload: String, signature: String, secret: String): Boolean {
        val expectedSignature = sign(payload, secret)
        return expectedSignature == signature
    }
    
    /**
     * Verify a hex-encoded HMAC-SHA256 signature.
     *
     * @param payload The original payload
     * @param signature The hex-encoded signature to verify
     * @param secret The secret key
     * @return True if the signature is valid
     */
    fun verifyHex(payload: String, signature: String, secret: String): Boolean {
        val expectedSignature = signHex(payload, secret)
        return expectedSignature.equals(signature, ignoreCase = true)
    }
}
