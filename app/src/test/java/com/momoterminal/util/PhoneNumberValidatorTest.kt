package com.momoterminal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PhoneNumberValidator.
 */
class PhoneNumberValidatorTest {

    @Test
    fun `validate returns error for blank phone number`() {
        val result = PhoneNumberValidator.validate("")
        
        assertFalse(result.isValid)
        assertEquals("Phone number cannot be empty", result.errorMessage)
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

    // Basic validation tests

    @Test
    fun `validate returns Invalid for empty string`() {
        val result = PhoneNumberValidator.validate("")
        assertFalse(result.isValid)
        assertEquals("Phone number cannot be empty", result.errorMessage)
    }

    @Test
    fun `validate returns Invalid for blank string`() {
        val result = PhoneNumberValidator.validate("   ")
        assertFalse(result.isValid)
    }

    // E.164 format tests

    @Test
    fun `validate accepts valid E164 format with plus sign`() {
        val result = PhoneNumberValidator.validate("+250788767816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    @Test
    fun `validate accepts E164 format without plus and adds default country code`() {
        val result = PhoneNumberValidator.validate("788767816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    // Local format tests

    @Test
    fun `validate converts local format with leading zero to E164`() {
        val result = PhoneNumberValidator.validate("0788767816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    @Test
    fun `validate handles 00 international prefix`() {
        val result = PhoneNumberValidator.validate("00250788767816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    // Cleaning tests

    @Test
    fun `validate removes spaces from phone number`() {
        val result = PhoneNumberValidator.validate("+250 788 767 816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    @Test
    fun `validate removes dashes from phone number`() {
        val result = PhoneNumberValidator.validate("+250-788-767-816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    @Test
    fun `validate removes parentheses from phone number`() {
        val result = PhoneNumberValidator.validate("(+250) 788767816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    // Invalid format tests

    @Test
    fun `validate returns Invalid for too short number`() {
        val result = PhoneNumberValidator.validate("+1234")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("short") == true)
    }

    @Test
    fun `validate returns Invalid for number with letters`() {
        val result = PhoneNumberValidator.validate("+250abc123")
        assertFalse(result.isValid)
    }

    // Rwanda specific tests

    @Test
    fun `validateRwandaNumber accepts valid MTN number`() {
        val result = PhoneNumberValidator.validateRwandaNumber("0788767816")
        assertTrue(result.isValid)
        assertEquals("+250788767816", result.formattedNumber)
    }

    @Test
    fun `validateRwandaNumber accepts valid Airtel number`() {
        val result = PhoneNumberValidator.validateRwandaNumber("0728767816")
        assertTrue(result.isValid)
        assertEquals("+250728767816", result.formattedNumber)
    }

    @Test
    fun `validateRwandaNumber accepts valid 079 prefix`() {
        val result = PhoneNumberValidator.validateRwandaNumber("0798767816")
        assertTrue(result.isValid)
    }

    @Test
    fun `validateRwandaNumber rejects invalid prefix`() {
        val result = PhoneNumberValidator.validateRwandaNumber("0688767816")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("prefix") == true)
    }

    @Test
    fun `validateRwandaNumber rejects non-Rwanda country code`() {
        val result = PhoneNumberValidator.validateRwandaNumber("+1234567890")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("Rwanda") == true)
    }

    @Test
    fun `validateRwandaNumber rejects too short number`() {
        val result = PhoneNumberValidator.validateRwandaNumber("078876781")
        assertFalse(result.isValid)
    }

    @Test
    fun `validateRwandaNumber rejects too long number`() {
        val result = PhoneNumberValidator.validateRwandaNumber("07887678166")
        assertFalse(result.isValid)
    }

    // cleanPhoneNumber tests

    @Test
    fun `cleanPhoneNumber removes all non-digit characters except leading plus`() {
        val result = PhoneNumberValidator.cleanPhoneNumber("+250 (788) 767-816")
        assertEquals("+250788767816", result)
    }

    @Test
    fun `cleanPhoneNumber preserves leading plus`() {
        val result = PhoneNumberValidator.cleanPhoneNumber("+250788767816")
        assertEquals("+250788767816", result)
    }

    @Test
    fun `cleanPhoneNumber trims whitespace`() {
        val result = PhoneNumberValidator.cleanPhoneNumber("  0788767816  ")
        assertEquals("0788767816", result)
    }

    // formatForDisplay tests

    @Test
    fun `formatForDisplay formats Rwanda number correctly`() {
        val result = PhoneNumberValidator.formatForDisplay("+250788767816")
        assertEquals("+250 78 876 7816", result)
    }

    @Test
    fun `formatForDisplay returns original for non-Rwanda numbers`() {
        val result = PhoneNumberValidator.formatForDisplay("+12025551234")
        assertEquals("+12025551234", result)
    }

    @Test
    fun `formatForDisplay returns original for short numbers`() {
        val result = PhoneNumberValidator.formatForDisplay("+1234")
        assertEquals("+1234", result)
    }

    // isLikelyValid tests

    @Test
    fun `isLikelyValid returns true for valid E164 number`() {
        val result = PhoneNumberValidator.isLikelyValid("+250788767816")
        assertTrue(result)
    }

    @Test
    fun `isLikelyValid returns true for digits-only number of valid length`() {
        val result = PhoneNumberValidator.isLikelyValid("788767816")
        assertTrue(result)
    }

    @Test
    fun `isLikelyValid returns false for too short number`() {
        val result = PhoneNumberValidator.isLikelyValid("1234")
        assertFalse(result)
    }

    @Test
    fun `isLikelyValid returns false for number with letters`() {
        val result = PhoneNumberValidator.isLikelyValid("+250abc")
        assertFalse(result)
    }

    // Custom country code tests

    @Test
    fun `validate uses custom country code when provided`() {
        val result = PhoneNumberValidator.validate("5551234567", "+1")
        assertTrue(result.isValid)
        assertEquals("+15551234567", result.formattedNumber)
    }
}
