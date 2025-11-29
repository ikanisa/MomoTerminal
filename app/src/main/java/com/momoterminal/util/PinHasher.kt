package com.momoterminal.util

import java.security.MessageDigest

/**
 * Utility object for securely hashing PINs before transmission.
 * Uses SHA-256 with a salt for secure PIN hashing.
 */
object PinHasher {
    
    /**
     * Hashes a PIN using SHA-256 with the phone number as salt.
     * This ensures the same PIN produces different hashes for different users.
     *
     * @param pin The 6-digit PIN to hash
     * @param salt The salt value (typically the phone number)
     * @return Hexadecimal string of the hash
     */
    fun hash(pin: String, salt: String): String {
        val combined = "$pin$salt"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Hashes a PIN with multiple iterations for added security.
     * Uses PBKDF2-like stretching.
     *
     * @param pin The 6-digit PIN to hash
     * @param salt The salt value (typically the phone number)
     * @param iterations Number of hash iterations (default: 1000)
     * @return Hexadecimal string of the hash
     */
    fun hashWithIterations(pin: String, salt: String, iterations: Int = 1000): String {
        var result = "$pin$salt"
        val digest = MessageDigest.getInstance("SHA-256")
        
        repeat(iterations) {
            result = digest.digest(result.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
        
        return result
    }
    
    /**
     * Verifies if a PIN matches a previously hashed value.
     *
     * @param pin The PIN to verify
     * @param salt The salt used during hashing
     * @param hashedPin The previously hashed PIN to compare against
     * @return true if the PIN matches, false otherwise
     */
    fun verify(pin: String, salt: String, hashedPin: String): Boolean {
        return hash(pin, salt) == hashedPin
    }
    
    /**
     * Verifies if a PIN matches a previously hashed value with iterations.
     *
     * @param pin The PIN to verify
     * @param salt The salt used during hashing
     * @param hashedPin The previously hashed PIN to compare against
     * @param iterations Number of hash iterations used during hashing
     * @return true if the PIN matches, false otherwise
     */
    fun verifyWithIterations(
        pin: String, 
        salt: String, 
        hashedPin: String, 
        iterations: Int = 1000
    ): Boolean {
        return hashWithIterations(pin, salt, iterations) == hashedPin
    }
}
