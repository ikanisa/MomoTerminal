package com.momoterminal.ussd

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.momoterminal.nfc.NfcPaymentData

/**
 * Helper class for generating and launching USSD codes.
 */
object UssdHelper {

    /**
     * Generate a USSD dial string for the specified provider.
     * Note: USSD codes vary by country and provider. This is a generic implementation.
     */
    fun generateUssdCode(provider: NfcPaymentData.Provider, merchantCode: String, amount: Double): String {
        val formattedAmount = "%.2f".format(amount)
        
        // Generic USSD format - actual codes depend on country/provider
        return when (provider) {
            NfcPaymentData.Provider.MTN -> "*170*1*1*$merchantCode*$formattedAmount#"
            NfcPaymentData.Provider.VODAFONE -> "*110*1*$merchantCode*$formattedAmount#"
            NfcPaymentData.Provider.VODACOM -> "*150*00#" // M-Pesa varies by country
            NfcPaymentData.Provider.AIRTEL -> "*500*1*$merchantCode*$formattedAmount#"
            NfcPaymentData.Provider.ORANGE -> "*144#"
            NfcPaymentData.Provider.TIGO -> "*150*01#"
            NfcPaymentData.Provider.WAVE -> "*228#"
            NfcPaymentData.Provider.MOOV -> "*155#"
        }
    }

    /**
     * Create a dial Intent that shows the dialer with USSD code.
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
     */
    fun launchUssdDialer(context: Context, ussdCode: String): Boolean {
        return try {
            context.startActivity(createDialIntent(ussdCode))
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
        provider: NfcPaymentData.Provider,
        merchantCode: String,
        amount: Double,
        currency: String
    ): NfcPaymentData {
        return NfcPaymentData(
            merchantPhone = merchantCode,
            amountInMinorUnits = (amount * 100).toLong(),
            currency = currency,
            provider = provider
        )
    }
}
