package com.momoterminal.util

/**
 * Utility object for validating and formatting phone numbers.
 * Supports international phone number formats for WhatsApp OTP.
 */
object PhoneNumberValidator {
    
    // Valid phone number pattern: +<country_code><number> (9-15 digits total)
    private val PHONE_PATTERN = Regex("^\\+?[1-9]\\d{9,14}$")
    
    // Rwanda phone number pattern (starts with 250 followed by 9 digits)
    private val RWANDA_PATTERN = Regex("^(\\+?250)?[0-9]{9}$")
    
    /**
     * Validation result containing status and any error message.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val formattedNumber: String? = null
    )
    
    /**
     * Validates if the given phone number is in a valid format.
     *
     * @param phoneNumber The phone number to validate
     * @return ValidationResult with status and formatted number if valid
     */
    fun validate(phoneNumber: String): ValidationResult {
        if (phoneNumber.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Phone number is required"
            )
        }
        
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        if (cleaned.length < 9) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Phone number is too short"
            )
        }
        
        if (cleaned.length > 15) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Phone number is too long"
            )
        }
        
        val formatted = formatForWhatsApp(cleaned)
        
        if (!PHONE_PATTERN.matches(formatted)) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Please enter a valid phone number with country code"
            )
        }
        
        return ValidationResult(
            isValid = true,
            formattedNumber = formatted
        )
    }
    
    /**
     * Checks if the phone number is valid without returning detailed result.
     *
     * @param phoneNumber The phone number to check
     * @return true if valid, false otherwise
     */
    fun isValid(phoneNumber: String): Boolean {
        return validate(phoneNumber).isValid
    }
    
    /**
     * Cleans the phone number by removing all non-numeric characters except '+'.
     *
     * @param phoneNumber The phone number to clean
     * @return Cleaned phone number
     */
    fun cleanPhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }
    
    /**
     * Formats a phone number for WhatsApp (E.164 format).
     * Ensures the number starts with '+'.
     *
     * @param phoneNumber The phone number to format
     * @return Formatted phone number in E.164 format
     */
    fun formatForWhatsApp(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        // If already has country code with +
        if (cleaned.startsWith("+")) {
            return cleaned
        }
        
        // Handle Rwanda numbers (starts with 0 or 7)
        if (cleaned.startsWith("0") && cleaned.length == 10) {
            // Convert 0788... to +250788...
            return "+250${cleaned.substring(1)}"
        }
        
        if (cleaned.startsWith("7") && cleaned.length == 9) {
            // Convert 788... to +250788...
            return "+250$cleaned"
        }
        
        // If starts with country code without +
        if (cleaned.startsWith("250") && cleaned.length == 12) {
            return "+$cleaned"
        }
        
        // Default: add + prefix
        return "+$cleaned"
    }
    
    /**
     * Masks the phone number for display (e.g., +250788***816).
     *
     * @param phoneNumber The phone number to mask
     * @return Masked phone number
     */
    fun maskPhoneNumber(phoneNumber: String): String {
        val cleaned = cleanPhoneNumber(phoneNumber)
        if (cleaned.length < 6) return cleaned
        
        val visibleStart = if (cleaned.startsWith("+")) 7 else 6
        val visibleEnd = 3
        
        if (cleaned.length <= visibleStart + visibleEnd) return cleaned
        
        val masked = cleaned.substring(0, visibleStart) +
            "*".repeat(cleaned.length - visibleStart - visibleEnd) +
            cleaned.substring(cleaned.length - visibleEnd)
        
        return masked
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for validating and formatting phone numbers.
 * Supports Rwanda (+250) and other international formats.
 */
@Singleton
class PhoneNumberValidator @Inject constructor() {
    
    companion object {
        // Rwanda country code
        private const val RWANDA_COUNTRY_CODE = "+250"
        
        // Phone number length constraints
        private const val MIN_PHONE_LENGTH = 9
        private const val MAX_PHONE_LENGTH = 15
        
        // Rwanda phone prefixes (after country code)
        private val RWANDA_MOBILE_PREFIXES = listOf("78", "79", "72", "73")
        
        // Regex patterns
        private val E164_PATTERN = Regex("^\\+[1-9]\\d{1,14}$")
        private val DIGITS_ONLY_PATTERN = Regex("^\\d+$")
    }
    
    /**
     * Validation result for phone numbers.
     */
    sealed class ValidationResult {
        data class Valid(val formattedNumber: String) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
    
    /**
     * Validate and format a phone number for WhatsApp API.
     * Returns a formatted E.164 number (e.g., +250788767816) or an error.
     *
     * @param phoneNumber The phone number to validate
     * @param defaultCountryCode The default country code to use if not provided (defaults to Rwanda +250)
     * @return ValidationResult indicating success with formatted number or failure with reason
     */
    fun validate(phoneNumber: String, defaultCountryCode: String = RWANDA_COUNTRY_CODE): ValidationResult {
        // Clean the phone number - remove spaces, dashes, parentheses
        val cleaned = cleanPhoneNumber(phoneNumber)
        
        if (cleaned.isBlank()) {
            return ValidationResult.Invalid("Phone number cannot be empty")
        }
        
        // Check minimum length before processing
        if (cleaned.length < MIN_PHONE_LENGTH) {
            return ValidationResult.Invalid("Phone number is too short")
        }
        
        // Check if it starts with +
        val formatted = when {
            cleaned.startsWith("+") -> {
                // Already has country code, validate format
                if (!E164_PATTERN.matches(cleaned)) {
                    return ValidationResult.Invalid("Invalid phone number format")
                }
                cleaned
            }
            cleaned.startsWith("00") -> {
                // International format with 00 prefix
                val withPlus = "+" + cleaned.substring(2)
                if (!E164_PATTERN.matches(withPlus)) {
                    return ValidationResult.Invalid("Invalid phone number format")
                }
                withPlus
            }
            cleaned.startsWith("0") -> {
                // Local format starting with 0, add country code
                val withoutLeadingZero = cleaned.substring(1)
                val withCountryCode = defaultCountryCode + withoutLeadingZero
                if (!E164_PATTERN.matches(withCountryCode)) {
                    return ValidationResult.Invalid("Invalid phone number format")
                }
                withCountryCode
            }
            DIGITS_ONLY_PATTERN.matches(cleaned) -> {
                // Plain digits, add country code
                val withCountryCode = defaultCountryCode + cleaned
                if (!E164_PATTERN.matches(withCountryCode)) {
                    return ValidationResult.Invalid("Invalid phone number format")
                }
                withCountryCode
            }
            else -> {
                return ValidationResult.Invalid("Phone number contains invalid characters")
            }
        }
        
        // Validate max length (E164 pattern already validates min length)
        if (formatted.length > MAX_PHONE_LENGTH) {
            return ValidationResult.Invalid("Phone number is too long")
        }
        
        return ValidationResult.Valid(formatted)
    }
    
    /**
     * Validate specifically for Rwanda phone numbers.
     *
     * @param phoneNumber The phone number to validate
     * @return ValidationResult indicating success or failure
     */
    fun validateRwandaNumber(phoneNumber: String): ValidationResult {
        val result = validate(phoneNumber, RWANDA_COUNTRY_CODE)
        
        if (result is ValidationResult.Valid) {
            // Verify it's a Rwanda number
            if (!result.formattedNumber.startsWith(RWANDA_COUNTRY_CODE)) {
                return ValidationResult.Invalid("Please enter a Rwanda phone number")
            }
            
            // Verify it's a valid Rwanda mobile prefix
            val numberPart = result.formattedNumber.substring(RWANDA_COUNTRY_CODE.length)
            val hasValidPrefix = RWANDA_MOBILE_PREFIXES.any { numberPart.startsWith(it) }
            
            if (!hasValidPrefix) {
                return ValidationResult.Invalid("Invalid Rwanda mobile number prefix")
            }
            
            // Rwanda mobile numbers should be exactly 9 digits after country code
            if (numberPart.length != 9) {
                return ValidationResult.Invalid("Rwanda mobile numbers must be 9 digits")
            }
        }
        
        return result
    }
    
    /**
     * Clean a phone number by removing non-essential characters.
     *
     * @param phoneNumber The raw phone number input
     * @return Cleaned phone number with only digits and optional leading +
     */
    fun cleanPhoneNumber(phoneNumber: String): String {
        // Preserve the leading + if present, remove all other non-digit characters
        val trimmed = phoneNumber.trim()
        return if (trimmed.startsWith("+")) {
            "+" + trimmed.substring(1).filter { it.isDigit() }
        } else {
            trimmed.filter { it.isDigit() }
        }
    }
    
    /**
     * Format a phone number for display (e.g., +250 788 767 816).
     *
     * @param phoneNumber The E.164 formatted phone number
     * @return Formatted display string
     */
    fun formatForDisplay(phoneNumber: String): String {
        if (phoneNumber.length < 10) return phoneNumber
        
        return when {
            phoneNumber.startsWith(RWANDA_COUNTRY_CODE) -> {
                val local = phoneNumber.substring(RWANDA_COUNTRY_CODE.length)
                if (local.length == 9) {
                    "$RWANDA_COUNTRY_CODE ${local.substring(0, 2)} ${local.substring(2, 5)} ${local.substring(5)}"
                } else {
                    phoneNumber
                }
            }
            else -> phoneNumber
        }
    }
    
    /**
     * Check if a phone number is likely valid (quick check without formatting).
     *
     * @param phoneNumber The phone number to check
     * @return true if the phone number appears valid
     */
    fun isLikelyValid(phoneNumber: String): Boolean {
        val cleaned = cleanPhoneNumber(phoneNumber)
        return cleaned.length >= MIN_PHONE_LENGTH && 
               (E164_PATTERN.matches(cleaned) || DIGITS_ONLY_PATTERN.matches(cleaned))
    }
}
