package com.momoterminal.nfc

import android.content.Intent
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.momoterminal.api.NfcPaymentData

/**
 * Host Card Emulation (HCE) Service for Mobile Money payments.
 * 
 * This service turns the merchant's phone into an NFC tag that customers
 * can tap to receive payment information. When a customer taps their NFC-enabled
 * phone, they receive the USSD dial string to complete the payment.
 */
class MomoHceService : HostApduService() {

    private var currentPaymentData: NfcPaymentData? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MomoHceService created")
    }

    /**
     * Called when a command APDU is received from a reader.
     * The service processes SELECT and READ commands to transmit payment data.
     */
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.d(TAG, "Received APDU: ${commandApdu.toHexString()}")

        // Parse the command APDU
        if (commandApdu.isEmpty()) {
            return buildErrorResponse(SW_UNKNOWN)
        }

        // Check if this is a SELECT command
        if (isSelectCommand(commandApdu)) {
            Log.d(TAG, "Processing SELECT command")
            return handleSelectCommand(commandApdu)
        }

        // Check if this is a READ command
        if (isReadCommand(commandApdu)) {
            Log.d(TAG, "Processing READ command")
            return handleReadCommand()
        }

        return buildErrorResponse(SW_INS_NOT_SUPPORTED)
    }

    /**
     * Handle SELECT AID command.
     */
    private fun handleSelectCommand(commandApdu: ByteArray): ByteArray {
        // Extract the AID from the command
        val aidLength = commandApdu.getOrNull(4)?.toInt() ?: 0
        if (aidLength > 0 && commandApdu.size >= 5 + aidLength) {
            val aid = commandApdu.sliceArray(5 until 5 + aidLength)
            Log.d(TAG, "AID selected: ${aid.toHexString()}")
        }

        // Return success with the current payment data if available
        return if (currentPaymentData != null) {
            buildSuccessResponse(getPaymentPayload())
        } else {
            buildSuccessResponse(byteArrayOf()) // No data available
        }
    }

    /**
     * Handle READ command to return payment data.
     */
    private fun handleReadCommand(): ByteArray {
        return if (currentPaymentData != null) {
            buildSuccessResponse(getPaymentPayload())
        } else {
            buildErrorResponse(SW_FILE_NOT_FOUND)
        }
    }

    /**
     * Get the payment payload as bytes.
     * Returns NDEF message containing the USSD dial string.
     */
    private fun getPaymentPayload(): ByteArray {
        val paymentData = currentPaymentData ?: return byteArrayOf()
        
        // Create an NDEF record with the USSD dial string
        // Format: tel:*170*1*1*merchantCode*amount#
        val ussdString = paymentData.toNdefPayload()
        val ndefMessage = createNdefMessage(ussdString)
        
        Log.d(TAG, "Sending USSD: $ussdString")
        return ndefMessage
    }

    /**
     * Creates a simple NDEF message with URI record for tel: scheme.
     */
    private fun createNdefMessage(ussdString: String): ByteArray {
        // NDEF message structure:
        // - NDEF header
        // - URI record with tel: scheme
        
        val uriPayload = ussdString.substringAfter("tel:")
        val uriBytes = uriPayload.toByteArray(Charsets.UTF_8)
        
        // NDEF record header for URI
        val tnf: Byte = 0x01 // TNF = Well-known
        val typeLength: Byte = 0x01 // Type length = 1
        val type: Byte = 0x55 // 'U' for URI
        val uriIdentifier: Byte = 0x05 // tel: scheme identifier
        
        val payloadLength = uriBytes.size + 1 // +1 for URI identifier
        
        // Build NDEF record
        val ndefRecord = ByteArray(4 + 1 + payloadLength) // header + type + payload
        ndefRecord[0] = (0xD1).toByte() // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=001
        ndefRecord[1] = typeLength
        ndefRecord[2] = payloadLength.toByte()
        ndefRecord[3] = type
        ndefRecord[4] = uriIdentifier
        System.arraycopy(uriBytes, 0, ndefRecord, 5, uriBytes.size)
        
        return ndefRecord
    }

    /**
     * Check if the command is a SELECT command.
     */
    private fun isSelectCommand(commandApdu: ByteArray): Boolean {
        // SELECT command: CLA=00, INS=A4
        return commandApdu.size >= 2 && 
               commandApdu[0] == 0x00.toByte() && 
               commandApdu[1] == 0xA4.toByte()
    }

    /**
     * Check if the command is a READ command.
     */
    private fun isReadCommand(commandApdu: ByteArray): Boolean {
        // READ BINARY command: CLA=00, INS=B0
        return commandApdu.size >= 2 && 
               commandApdu[0] == 0x00.toByte() && 
               commandApdu[1] == 0xB0.toByte()
    }

    /**
     * Build a success response with data.
     */
    private fun buildSuccessResponse(data: ByteArray): ByteArray {
        val response = ByteArray(data.size + 2)
        System.arraycopy(data, 0, response, 0, data.size)
        response[data.size] = SW_OK[0]
        response[data.size + 1] = SW_OK[1]
        return response
    }

    /**
     * Build an error response.
     */
    private fun buildErrorResponse(statusWord: ByteArray): ByteArray {
        return statusWord.clone()
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "HCE deactivated, reason: $reason")
    }

    /**
     * Called when the service receives a new payment amount from the activity.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ACTION_SET_PAYMENT_DATA) {
                val amount = it.getDoubleExtra(EXTRA_AMOUNT, 0.0)
                val merchantCode = it.getStringExtra(EXTRA_MERCHANT_CODE) ?: ""
                val ussdCode = it.getStringExtra(EXTRA_USSD_CODE) ?: ""
                
                if (amount > 0 && merchantCode.isNotEmpty()) {
                    currentPaymentData = NfcPaymentData(
                        amount = amount,
                        merchantCode = merchantCode,
                        ussdCode = ussdCode
                    )
                    Log.d(TAG, "Payment data set: $currentPaymentData")
                }
            } else if (it.action == ACTION_CLEAR_PAYMENT_DATA) {
                currentPaymentData = null
                Log.d(TAG, "Payment data cleared")
            }
        }
        return START_STICKY
    }

    companion object {
        private const val TAG = "MomoHceService"
        
        // Status words
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_UNKNOWN = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        private val SW_INS_NOT_SUPPORTED = byteArrayOf(0x6D.toByte(), 0x00.toByte())
        private val SW_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        
        // Intent actions
        const val ACTION_SET_PAYMENT_DATA = "com.momoterminal.action.SET_PAYMENT_DATA"
        const val ACTION_CLEAR_PAYMENT_DATA = "com.momoterminal.action.CLEAR_PAYMENT_DATA"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT_CODE = "extra_merchant_code"
        const val EXTRA_USSD_CODE = "extra_ussd_code"
    }
}

/**
 * Extension function to convert ByteArray to hex string.
 */
private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02X".format(it) }
}
