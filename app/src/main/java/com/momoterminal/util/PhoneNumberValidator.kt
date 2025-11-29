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
    }
}
