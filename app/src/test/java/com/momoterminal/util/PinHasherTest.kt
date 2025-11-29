package com.momoterminal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PinHasher.
 */
class PinHasherTest {

    @Test
    fun `hash produces consistent results for same input`() {
        val pin = "123456"
        val salt = "+250788123456"
        
        val hash1 = PinHasher.hash(pin, salt)
        val hash2 = PinHasher.hash(pin, salt)
        
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hash produces different results for different pins`() {
        val salt = "+250788123456"
        
        val hash1 = PinHasher.hash("123456", salt)
        val hash2 = PinHasher.hash("654321", salt)
        
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hash produces different results for different salts`() {
        val pin = "123456"
        
        val hash1 = PinHasher.hash(pin, "+250788123456")
        val hash2 = PinHasher.hash(pin, "+250788654321")
        
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `hash produces 64 character hex string`() {
        val hash = PinHasher.hash("123456", "+250788123456")
        
        // SHA-256 produces 32 bytes = 64 hex characters
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `hashWithIterations produces different result than single hash`() {
        val pin = "123456"
        val salt = "+250788123456"
        
        val singleHash = PinHasher.hash(pin, salt)
        val iteratedHash = PinHasher.hashWithIterations(pin, salt, 1000)
        
        assertNotEquals(singleHash, iteratedHash)
    }

    @Test
    fun `hashWithIterations is consistent`() {
        val pin = "123456"
        val salt = "+250788123456"
        
        val hash1 = PinHasher.hashWithIterations(pin, salt, 100)
        val hash2 = PinHasher.hashWithIterations(pin, salt, 100)
        
        assertEquals(hash1, hash2)
    }

    @Test
    fun `verify returns true for matching pin`() {
        val pin = "123456"
        val salt = "+250788123456"
        val hashedPin = PinHasher.hash(pin, salt)
        
        assertTrue(PinHasher.verify(pin, salt, hashedPin))
    }

    @Test
    fun `verify returns false for non-matching pin`() {
        val salt = "+250788123456"
        val hashedPin = PinHasher.hash("123456", salt)
        
        assertFalse(PinHasher.verify("654321", salt, hashedPin))
    }

    @Test
    fun `verifyWithIterations returns true for matching pin`() {
        val pin = "123456"
        val salt = "+250788123456"
        val iterations = 500
        val hashedPin = PinHasher.hashWithIterations(pin, salt, iterations)
        
        assertTrue(PinHasher.verifyWithIterations(pin, salt, hashedPin, iterations))
    }

    @Test
    fun `verifyWithIterations returns false for wrong iteration count`() {
        val pin = "123456"
        val salt = "+250788123456"
        val hashedPin = PinHasher.hashWithIterations(pin, salt, 500)
        
        assertFalse(PinHasher.verifyWithIterations(pin, salt, hashedPin, 1000))
    }
}
