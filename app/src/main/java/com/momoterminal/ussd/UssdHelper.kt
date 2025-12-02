package com.momoterminal.ussd

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.momoterminal.nfc.NfcPaymentData

/**
 * Helper class for generating and launching USSD codes.
 * 
 * This utility helps create properly formatted USSD dial strings
 * for various mobile money providers and can launch the phone dialer
 * with the pre-filled USSD code.
 */
object UssdHelper {

    /**
     * Supported mobile money providers in Ghana.
     */
    enum class Provider {
        MTN_MOMO,
        VODAFONE_CASH,
        AIRTELTIGO_MONEY
    }

    /**
     * Generate a USSD dial string for the specified provider.
     * 
     * @param provider The mobile money provider
     * @param merchantCode The merchant's code/number
     * @param amount The payment amount
     * @return Formatted USSD dial string
     */
    fun generateUssdCode(provider: Provider, merchantCode: String, amount: Double): String {
        val formattedAmount = "%.2f".format(amount)
        
        return when (provider) {
            Provider.MTN_MOMO -> {
                // MTN Mobile Money: *170*1*1*merchantCode*amount#
                "*170*1*1*$merchantCode*$formattedAmount#"
            }
            Provider.VODAFONE_CASH -> {
                // Vodafone Cash: *110*1*merchantCode*amount#
                "*110*1*$merchantCode*$formattedAmount#"
            }
            Provider.AIRTELTIGO_MONEY -> {
                // AirtelTigo Money: *500*1*merchantCode*amount#
                "*500*1*$merchantCode*$formattedAmount#"
            }
        }
    }

    /**
     * Generate a USSD dial string with a generic format.
     * This can be customized based on specific requirements.
     * 
     * @param baseCode The base USSD code (e.g., "*170*1*1*")
     * @param merchantCode The merchant's code
     * @param amount The payment amount
     * @return Formatted USSD dial string
     */
    fun generateCustomUssd(baseCode: String, merchantCode: String, amount: Double): String {
        val formattedAmount = "%.2f".format(amount)
        return "$baseCode$merchantCode*$formattedAmount#"
    }

    /**
     * Create a dial Intent that shows the dialer with USSD code.
     * Uses ACTION_DIAL which doesn't require CALL_PHONE permission.
     * 
     * @param ussdCode The USSD code to show in dialer
     * @return Intent that shows the dialer with the USSD code
     */
    fun createDialIntent(ussdCode: String): Intent {
        val encodedUssd = Uri.encode(ussdCode)
        val dialUri = Uri.parse("tel:$encodedUssd")
        
        return Intent(Intent.ACTION_DIAL, dialUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Launch the USSD dialer.
     * Shows the dialer with USSD code pre-filled (user must press call).
     * 
     * @param context The context
     * @param ussdCode The USSD code to dial
     * @return true if successfully launched
     */
    fun launchUssdDialer(context: Context, ussdCode: String): Boolean {
        return try {
            val intent = createDialIntent(ussdCode)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create NfcPaymentData from provider, merchant code and amount.
     */
    fun createPaymentData(
        provider: Provider,
        merchantCode: String,
        amount: Double
    ): NfcPaymentData {
        val ussdCode = generateUssdCode(provider, merchantCode, amount)
        val amountInPesewas = (amount * 100).toLong() // Convert to pesewas
        return NfcPaymentData(
            merchantPhone = merchantCode,
            amountInPesewas = amountInPesewas,
            provider = when (provider) {
                Provider.MTN_MOMO -> NfcPaymentData.Provider.MTN
                Provider.VODAFONE_CASH -> NfcPaymentData.Provider.VODAFONE
                Provider.AIRTELTIGO_MONEY -> NfcPaymentData.Provider.AIRTEL_TIGO
            }
        )
    }
}
