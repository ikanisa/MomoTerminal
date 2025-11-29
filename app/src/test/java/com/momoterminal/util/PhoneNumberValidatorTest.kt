package com.momoterminal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PhoneNumberValidator.
 */
class PhoneNumberValidatorTest {

    @Test
    fun `validate returns error for blank phone number`() {
        val result = PhoneNumberValidator.validate("")
        
        assertFalse(result.isValid)
        assertEquals("Phone number is required", result.errorMessage)
        assertNull(result.formattedNumber)
    }

    @Test
    fun `validate returns error for phone number too short`() {
        val result = PhoneNumberValidator.validate("12345678") // 8 digits
        
        assertFalse(result.isValid)
        assertEquals("Phone number is too short", result.errorMessage)
    }

    @Test
    fun `validate returns error for phone number too long`() {
        val result = PhoneNumberValidator.validate("+1234567890123456") // 16 digits
        
        assertFalse(result.isValid)
        assertEquals("Phone number is too long", result.errorMessage)
    }

    @Test
    fun `validate accepts valid international number with plus`() {
        val result = PhoneNumberValidator.validate("+250788123456")
        
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
        assertEquals("+250788123456", result.formattedNumber)
    }

    @Test
    fun `validate accepts valid Rwanda number without plus`() {
        val result = PhoneNumberValidator.validate("250788123456")
        
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
        assertEquals("+250788123456", result.formattedNumber)
    }

    @Test
    fun `validate formats Rwanda local number starting with 0`() {
        val result = PhoneNumberValidator.validate("0788123456")
        
        assertTrue(result.isValid)
        assertEquals("+250788123456", result.formattedNumber)
    }

    @Test
    fun `validate formats Rwanda number starting with 7`() {
        val result = PhoneNumberValidator.validate("788123456")
        
        assertTrue(result.isValid)
        assertEquals("+250788123456", result.formattedNumber)
    }

    @Test
    fun `isValid returns true for valid number`() {
        assertTrue(PhoneNumberValidator.isValid("+250788123456"))
    }

    @Test
    fun `isValid returns false for invalid number`() {
        assertFalse(PhoneNumberValidator.isValid(""))
        assertFalse(PhoneNumberValidator.isValid("123"))
    }

    @Test
    fun `cleanPhoneNumber removes non-numeric characters except plus`() {
        val result = PhoneNumberValidator.cleanPhoneNumber("+250 788-123-456")
        
        assertEquals("+250788123456", result)
    }

    @Test
    fun `cleanPhoneNumber removes parentheses and spaces`() {
        val result = PhoneNumberValidator.cleanPhoneNumber("(250) 788 123 456")
        
        assertEquals("250788123456", result)
    }

    @Test
    fun `formatForWhatsApp adds country code for local number`() {
        val result = PhoneNumberValidator.formatForWhatsApp("0788123456")
        
        assertEquals("+250788123456", result)
    }

    @Test
    fun `formatForWhatsApp keeps plus prefix`() {
        val result = PhoneNumberValidator.formatForWhatsApp("+1234567890")
        
        assertEquals("+1234567890", result)
    }

    @Test
    fun `formatForWhatsApp adds plus for country code without it`() {
        val result = PhoneNumberValidator.formatForWhatsApp("250788123456")
        
        assertEquals("+250788123456", result)
    }

    @Test
    fun `maskPhoneNumber masks middle digits`() {
        val result = PhoneNumberValidator.maskPhoneNumber("+250788123456")
        
        // Should show first 7 chars and last 3
        assertTrue(result.startsWith("+250788"))
        assertTrue(result.endsWith("456"))
        assertTrue(result.contains("*"))
    }

    @Test
    fun `maskPhoneNumber handles short numbers`() {
        val result = PhoneNumberValidator.maskPhoneNumber("12345")
        
        // Short numbers should be returned as-is
        assertEquals("12345", result)
    }
}
