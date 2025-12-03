package com.momoterminal.webhook

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for HmacSigner.
 * Uses Robolectric to provide Android Base64 implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class HmacSignerTest {
    
    private lateinit var hmacSigner: HmacSigner
    
    @Before
    fun setup() {
        hmacSigner = HmacSigner()
    }
    
    @Test
    fun `sign produces consistent output for same input`() {
        val payload = """{"message":"test","timestamp":1234567890}"""
        val secret = "my-secret-key"
        
        val signature1 = hmacSigner.sign(payload, secret)
        val signature2 = hmacSigner.sign(payload, secret)
        
        assertThat(signature1).isEqualTo(signature2)
    }
    
    @Test
    fun `sign produces different output for different secrets`() {
        val payload = """{"message":"test","timestamp":1234567890}"""
        val secret1 = "secret-1"
        val secret2 = "secret-2"
        
        val signature1 = hmacSigner.sign(payload, secret1)
        val signature2 = hmacSigner.sign(payload, secret2)
        
        assertThat(signature1).isNotEqualTo(signature2)
    }
    
    @Test
    fun `sign produces different output for different payloads`() {
        val payload1 = """{"message":"test1"}"""
        val payload2 = """{"message":"test2"}"""
        val secret = "my-secret-key"
        
        val signature1 = hmacSigner.sign(payload1, secret)
        val signature2 = hmacSigner.sign(payload2, secret)
        
        assertThat(signature1).isNotEqualTo(signature2)
    }
    
    @Test
    fun `signHex produces hex-encoded output`() {
        val payload = """{"message":"test"}"""
        val secret = "my-secret-key"
        
        val signature = hmacSigner.signHex(payload, secret)
        
        // Hex string should only contain hex characters
        assertThat(signature).matches("[0-9a-f]+")
        // HMAC-SHA256 produces 64 hex characters (32 bytes)
        assertThat(signature).hasLength(64)
    }
    
    @Test
    fun `verify returns true for valid signature`() {
        val payload = """{"message":"test","timestamp":1234567890}"""
        val secret = "my-secret-key"
        
        val signature = hmacSigner.sign(payload, secret)
        val isValid = hmacSigner.verify(payload, signature, secret)
        
        assertThat(isValid).isTrue()
    }
    
    @Test
    fun `verify returns false for invalid signature`() {
        val payload = """{"message":"test","timestamp":1234567890}"""
        val secret = "my-secret-key"
        val wrongSignature = "invalid-signature"
        
        val isValid = hmacSigner.verify(payload, wrongSignature, secret)
        
        assertThat(isValid).isFalse()
    }
    
    @Test
    fun `verify returns false for modified payload`() {
        val originalPayload = """{"message":"original"}"""
        val modifiedPayload = """{"message":"modified"}"""
        val secret = "my-secret-key"
        
        val signature = hmacSigner.sign(originalPayload, secret)
        val isValid = hmacSigner.verify(modifiedPayload, signature, secret)
        
        assertThat(isValid).isFalse()
    }
    
    @Test
    fun `verifyHex returns true for valid hex signature`() {
        val payload = """{"message":"test"}"""
        val secret = "my-secret-key"
        
        val signature = hmacSigner.signHex(payload, secret)
        val isValid = hmacSigner.verifyHex(payload, signature, secret)
        
        assertThat(isValid).isTrue()
    }
    
    @Test
    fun `verifyHex is case insensitive`() {
        val payload = """{"message":"test"}"""
        val secret = "my-secret-key"
        
        val signatureLower = hmacSigner.signHex(payload, secret).lowercase()
        val signatureUpper = hmacSigner.signHex(payload, secret).uppercase()
        
        assertThat(hmacSigner.verifyHex(payload, signatureLower, secret)).isTrue()
        assertThat(hmacSigner.verifyHex(payload, signatureUpper, secret)).isTrue()
    }
    
    @Test
    fun `sign handles empty payload`() {
        val payload = ""
        val secret = "my-secret-key"
        
        val signature = hmacSigner.sign(payload, secret)
        
        assertThat(signature).isNotEmpty()
    }
    
    @Test
    fun `sign handles unicode characters`() {
        val payload = """{"message":"こんにちは","amount":"€100"}"""
        val secret = "my-secret-key"
        
        val signature = hmacSigner.sign(payload, secret)
        val isValid = hmacSigner.verify(payload, signature, secret)
        
        assertThat(isValid).isTrue()
    }
    
    @Test
    fun `sign handles special characters in secret`() {
        val payload = """{"test":true}"""
        val secret = "secret!@#\$%^&*()_+-=[]{}|;':\",./<>?"
        
        val signature = hmacSigner.sign(payload, secret)
        val isValid = hmacSigner.verify(payload, signature, secret)
        
        assertThat(isValid).isTrue()
    }
    
    @Test
    fun `signHex produces known output for test vector`() {
        // Test with a known HMAC-SHA256 test vector
        val payload = "The quick brown fox jumps over the lazy dog"
        val secret = "key"
        
        val signature = hmacSigner.signHex(payload, secret)
        
        // Expected HMAC-SHA256 for this test vector
        assertThat(signature).isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8")
    }
}
