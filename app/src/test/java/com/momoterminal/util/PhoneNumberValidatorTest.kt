package com.momoterminal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PhoneNumberValidator.
 */
class PhoneNumberValidatorTest {

    private lateinit var validator: PhoneNumberValidator

    @Before
    fun setup() {
        validator = PhoneNumberValidator()
    }

    // Basic validation tests

    @Test
    fun `validate returns Invalid for empty string`() {
        val result = validator.validate("")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
        assertEquals("Phone number cannot be empty", (result as PhoneNumberValidator.ValidationResult.Invalid).reason)
    }

    @Test
    fun `validate returns Invalid for blank string`() {
        val result = validator.validate("   ")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
    }

    // E.164 format tests

    @Test
    fun `validate accepts valid E164 format with plus sign`() {
        val result = validator.validate("+250788767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    @Test
    fun `validate accepts E164 format without plus and adds default country code`() {
        val result = validator.validate("788767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    // Local format tests

    @Test
    fun `validate converts local format with leading zero to E164`() {
        val result = validator.validate("0788767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    @Test
    fun `validate handles 00 international prefix`() {
        val result = validator.validate("00250788767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    // Cleaning tests

    @Test
    fun `validate removes spaces from phone number`() {
        val result = validator.validate("+250 788 767 816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    @Test
    fun `validate removes dashes from phone number`() {
        val result = validator.validate("+250-788-767-816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    @Test
    fun `validate removes parentheses from phone number`() {
        val result = validator.validate("(+250) 788767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    // Invalid format tests

    @Test
    fun `validate returns Invalid for too short number`() {
        val result = validator.validate("+1234")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validate returns Invalid for number with letters`() {
        val result = validator.validate("+250abc123")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
    }

    // Rwanda specific tests

    @Test
    fun `validateRwandaNumber accepts valid MTN number`() {
        val result = validator.validateRwandaNumber("0788767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250788767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    @Test
    fun `validateRwandaNumber accepts valid Airtel number`() {
        val result = validator.validateRwandaNumber("0728767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+250728767816", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }

    @Test
    fun `validateRwandaNumber accepts valid 079 prefix`() {
        val result = validator.validateRwandaNumber("0798767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateRwandaNumber rejects invalid prefix`() {
        val result = validator.validateRwandaNumber("0688767816")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
        assertTrue((result as PhoneNumberValidator.ValidationResult.Invalid).reason.contains("prefix"))
    }

    @Test
    fun `validateRwandaNumber rejects non-Rwanda country code`() {
        val result = validator.validateRwandaNumber("+1234567890")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
        assertTrue((result as PhoneNumberValidator.ValidationResult.Invalid).reason.contains("Rwanda"))
    }

    @Test
    fun `validateRwandaNumber rejects too short number`() {
        val result = validator.validateRwandaNumber("078876781")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateRwandaNumber rejects too long number`() {
        val result = validator.validateRwandaNumber("07887678166")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Invalid)
    }

    // cleanPhoneNumber tests

    @Test
    fun `cleanPhoneNumber removes all non-digit characters except leading plus`() {
        val result = validator.cleanPhoneNumber("+250 (788) 767-816")
        assertEquals("+250788767816", result)
    }

    @Test
    fun `cleanPhoneNumber preserves leading plus`() {
        val result = validator.cleanPhoneNumber("+250788767816")
        assertEquals("+250788767816", result)
    }

    @Test
    fun `cleanPhoneNumber trims whitespace`() {
        val result = validator.cleanPhoneNumber("  0788767816  ")
        assertEquals("0788767816", result)
    }

    // formatForDisplay tests

    @Test
    fun `formatForDisplay formats Rwanda number correctly`() {
        val result = validator.formatForDisplay("+250788767816")
        assertEquals("+250 78 876 7816", result)
    }

    @Test
    fun `formatForDisplay returns original for non-Rwanda numbers`() {
        val result = validator.formatForDisplay("+12025551234")
        assertEquals("+12025551234", result)
    }

    @Test
    fun `formatForDisplay returns original for short numbers`() {
        val result = validator.formatForDisplay("+1234")
        assertEquals("+1234", result)
    }

    // isLikelyValid tests

    @Test
    fun `isLikelyValid returns true for valid E164 number`() {
        val result = validator.isLikelyValid("+250788767816")
        assertTrue(result)
    }

    @Test
    fun `isLikelyValid returns true for digits-only number of valid length`() {
        val result = validator.isLikelyValid("788767816")
        assertTrue(result)
    }

    @Test
    fun `isLikelyValid returns false for too short number`() {
        val result = validator.isLikelyValid("1234")
        assertFalse(result)
    }

    @Test
    fun `isLikelyValid returns false for number with letters`() {
        val result = validator.isLikelyValid("+250abc")
        assertFalse(result)
    }

    // Custom country code tests

    @Test
    fun `validate uses custom country code when provided`() {
        val result = validator.validate("5551234567", "+1")
        assertTrue(result is PhoneNumberValidator.ValidationResult.Valid)
        assertEquals("+15551234567", (result as PhoneNumberValidator.ValidationResult.Valid).formattedNumber)
    }
}
