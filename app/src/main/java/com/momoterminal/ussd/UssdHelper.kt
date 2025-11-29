package com.momoterminal.ussd

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.momoterminal.nfc.NfcPaymentData

/**
 * Helper class for generating and launching USSD codes.
 * 
 * This utility helps create properly formatted USSD dial strings
 * for various mobile money providers and can launch the phone dialer
 * with the pre-filled USSD code.
 * 
 * Note: All monetary amounts are in pesewas (smallest currency unit)
 * to avoid floating-point precision errors. 1 GHS = 100 pesewas.
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
     * @param amountInPesewas The payment amount in pesewas (1 GHS = 100 pesewas)
     * @return Formatted USSD dial string
     */
    fun generateUssdCode(provider: Provider, merchantCode: String, amountInPesewas: Long): String {
        val formattedAmount = "%.2f".format(amountInPesewas / 100.0)
        
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
     * @param amountInPesewas The payment amount in pesewas
     * @return Formatted USSD dial string
     */
    fun generateCustomUssd(baseCode: String, merchantCode: String, amountInPesewas: Long): String {
        val formattedAmount = "%.2f".format(amountInPesewas / 100.0)
        return "$baseCode$merchantCode*$formattedAmount#"
    }

    /**
     * Create a dial Intent for the USSD code.
     * 
     * @param ussdCode The USSD code to dial
     * @return Intent that launches the phone dialer with the USSD code
     */
    fun createDialIntent(ussdCode: String): Intent {
        // Encode the USSD code for URI (# becomes %23)
        val encodedUssd = Uri.encode(ussdCode)
        val dialUri = Uri.parse("tel:$encodedUssd")
        
        return Intent(Intent.ACTION_CALL, dialUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Create a view dial Intent (shows dialer without calling).
     * This is safer and doesn't require CALL_PHONE permission.
     * 
     * @param ussdCode The USSD code to show in dialer
     * @return Intent that shows the dialer with the USSD code
     */
    fun createViewDialIntent(ussdCode: String): Intent {
        val encodedUssd = Uri.encode(ussdCode)
        val dialUri = Uri.parse("tel:$encodedUssd")
        
        return Intent(Intent.ACTION_DIAL, dialUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Check if the app has permission to make calls.
     */
    fun hasCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Launch the USSD dialer.
     * 
     * @param context The context
     * @param ussdCode The USSD code to dial
     * @param directCall If true, calls directly (requires permission). 
     *                   If false, shows dialer with code pre-filled.
     * @return true if successfully launched
     */
    fun launchUssdDialer(context: Context, ussdCode: String, directCall: Boolean = false): Boolean {
        return try {
            val intent = if (directCall && hasCallPermission(context)) {
                createDialIntent(ussdCode)
            } else {
                createViewDialIntent(ussdCode)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create NfcPaymentData from provider, merchant code and amount.
     * 
     * @param provider The mobile money provider
     * @param merchantCode The merchant's phone number/code
     * @param amountInPesewas The payment amount in pesewas
     */
    fun createPaymentData(
        provider: Provider,
        merchantCode: String,
        amountInPesewas: Long
    ): NfcPaymentData {
        val nfcProvider = when (provider) {
            Provider.MTN_MOMO -> NfcPaymentData.Provider.MTN
            Provider.VODAFONE_CASH -> NfcPaymentData.Provider.VODAFONE
            Provider.AIRTELTIGO_MONEY -> NfcPaymentData.Provider.AIRTEL_TIGO
        }
        
        return NfcPaymentData(
            merchantPhone = merchantCode,
            amountInPesewas = amountInPesewas,
            provider = nfcProvider
        )
    }
}
