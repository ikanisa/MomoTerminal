package com.momoterminal.ussd

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.momoterminal.data.model.CountryConfig
import com.momoterminal.data.repository.CountryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for generating and launching USSD codes for mobile money transactions.
 * Handles country-specific USSD formats and special cases.
 */
@Singleton
class UssdService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val countryRepository: CountryRepository
) {
    companion object {
        private const val TAG = "UssdService"
    }

    sealed class UssdResult {
        data class Success(
            val ussdCode: String,
            val dialUri: String,
            val provider: String,
            val notes: String? = null
        ) : UssdResult()

        data class NoUssdSupport(
            val countryName: String,
            val alternativeMethod: String
        ) : UssdResult()

        data class Error(val message: String) : UssdResult()
    }

    /**
     * Generate USSD code to pay a merchant.
     */
    fun generateMerchantPaymentUssd(
        countryCode: String,
        merchantCode: String,
        amount: Double
    ): UssdResult {
        val country = countryRepository.getByCode(countryCode)
            ?: return UssdResult.Error("Country not found: $countryCode")

        if (!country.hasUssdSupport) {
            return UssdResult.NoUssdSupport(
                countryName = country.name,
                alternativeMethod = getAlternativeMethod(country)
            )
        }

        val formattedAmount = formatAmount(amount, country)
        val ussdCode = country.generateMerchantPaymentUssd(merchantCode, formattedAmount)
            ?: return UssdResult.Error("Failed to generate USSD code")

        return UssdResult.Success(
            ussdCode = ussdCode,
            dialUri = createDialUri(ussdCode),
            provider = country.providerName,
            notes = country.ussdNotes
        )
    }

    /**
     * Generate USSD code to send money to a phone number.
     */
    fun generateSendMoneyUssd(
        countryCode: String,
        phoneNumber: String,
        amount: Double
    ): UssdResult {
        val country = countryRepository.getByCode(countryCode)
            ?: return UssdResult.Error("Country not found: $countryCode")

        if (!country.hasUssdSupport) {
            return UssdResult.NoUssdSupport(
                countryName = country.name,
                alternativeMethod = getAlternativeMethod(country)
            )
        }

        val formattedAmount = formatAmount(amount, country)
        val cleanedPhone = cleanPhoneNumber(phoneNumber, country)

        // Handle special case: Togo where amount comes before phone
        val ussdCode = if (country.code == "TG") {
            country.ussdSendToPhone
                ?.replace("{amount}", formattedAmount)
                ?.replace("{phone}", cleanedPhone)
        } else {
            country.generateSendMoneyUssd(cleanedPhone, formattedAmount)
        } ?: return UssdResult.Error("Failed to generate USSD code")

        return UssdResult.Success(
            ussdCode = ussdCode,
            dialUri = createDialUri(ussdCode),
            provider = country.providerName,
            notes = country.ussdNotes
        )
    }

    /**
     * Launch USSD dialer with the generated code.
     */
    fun launchDialer(ussdCode: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse(createDialUri(ussdCode))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Launched dialer with USSD: $ussdCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch dialer", e)
            false
        }
    }

    /**
     * Launch USSD dialer directly (requires CALL_PHONE permission).
     */
    fun dialUssdDirectly(ussdCode: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse(createDialUri(ussdCode))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Directly dialed USSD: $ussdCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dial USSD directly", e)
            false
        }
    }

    /**
     * Get the base USSD code for a country's mobile money menu.
     */
    fun getBaseUssdCode(countryCode: String): String? =
        countryRepository.getByCode(countryCode)?.ussdBaseCode

    /**
     * Check balance USSD code (if available).
     */
    fun getBalanceCheckUssd(countryCode: String): String? =
        countryRepository.getByCode(countryCode)?.ussdCheckBalance

    private fun getAlternativeMethod(country: CountryConfig): String = when {
        country.hasAppSupport -> "Use the ${country.providerName} mobile app"
        country.hasQrSupport -> "Scan QR code with ${country.providerName}"
        else -> "Contact ${country.providerName} for payment options"
    }

    private fun formatAmount(amount: Double, country: CountryConfig): String =
        when (country.currencyDecimals) {
            0 -> "%.0f".format(amount)
            1 -> "%.1f".format(amount)
            else -> "%.2f".format(amount)
        }

    private fun cleanPhoneNumber(phone: String, country: CountryConfig): String {
        var cleaned = phone.replace(Regex("[^0-9]"), "")

        // Remove country prefix if present
        val prefixDigits = country.phonePrefix.replace("+", "")
        if (cleaned.startsWith(prefixDigits)) {
            cleaned = cleaned.substring(prefixDigits.length)
        }

        // Remove leading zero if present
        if (cleaned.startsWith("0") && cleaned.length > country.phoneLength) {
            cleaned = cleaned.substring(1)
        }

        return cleaned
    }

    private fun createDialUri(ussdCode: String): String = "tel:${Uri.encode(ussdCode)}"
}
