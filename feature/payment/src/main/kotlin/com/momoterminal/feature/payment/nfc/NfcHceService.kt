package com.momoterminal.feature.payment.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import com.momoterminal.core.common.config.AppConfig

/**
 * NFC Host Card Emulation (HCE) Service that emulates an NFC Type 4 Tag.
 * Broadcasts payment URIs via NDEF to NFC readers.
 * Reads merchant phone from AppConfig for dynamic configuration.
 */
class NfcHceService : HostApduService() {
    
    companion object {
        private const val TAG = "NfcHceService"
        
        // Status words
        private val SW_OK = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val SW_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val SW_ERROR = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        
        // File IDs
        private const val CC_FILE_ID: Short = 0xE103.toShort()
        private const val NDEF_FILE_ID: Short = 0xE104.toShort()
        
        // NFC Forum Type 4 Tag AID
        private val NDEF_TAG_AID = byteArrayOf(
            0xD2.toByte(), 0x76.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x85.toByte(), 0x01.toByte(), 0x01.toByte()
        )
        
        // Capability Container (CC) file for NDEF Type 4 Tag
        // CCLEN=15, Version=2.0, MLe=256, MLc=255, NDEF File Control TLV
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F,             // CCLEN - 15 bytes
            0x20,                   // Mapping Version 2.0
            0x00, 0xFF.toByte(),    // MLe - Maximum R-APDU data size (255)
            0x00, 0xFF.toByte(),    // MLc - Maximum C-APDU data size (255)
            0x04,                   // NDEF File Control TLV Type
            0x06,                   // Length of TLV value
            0xE1.toByte(), 0x04,    // NDEF File ID (E104)
            0x00, 0xFF.toByte(),    // Maximum NDEF file size (255)
            0x00,                   // Read access - no security
            0xFF.toByte()           // Write access - disabled
        )
        
        // SELECT command headers
        private const val INS_SELECT = 0xA4.toByte()
        private const val INS_READ_BINARY = 0xB0.toByte()
    }
    
    private var currentFile: Short = 0
    private var ndefMessage: ByteArray? = null
    
    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (commandApdu.isEmpty()) {
            return SW_ERROR
        }
        
        val ins = commandApdu[1]
        
        return when (ins) {
            INS_SELECT -> processSelect(commandApdu)
            INS_READ_BINARY -> processReadBinary(commandApdu)
            else -> {
                Log.d(TAG, "Unknown instruction: ${ins.toInt() and 0xFF}")
                SW_ERROR
            }
        }
    }
    
    private fun processSelect(apdu: ByteArray): ByteArray {
        if (apdu.size < 5) {
            return SW_ERROR
        }
        
        val p1 = apdu[2]
        val p2 = apdu[3]
        val lc = apdu[4].toInt() and 0xFF
        
        if (apdu.size < 5 + lc) {
            return SW_ERROR
        }
        
        val data = apdu.copyOfRange(5, 5 + lc)
        
        // Select by AID (P1=04, P2=00)
        if (p1 == 0x04.toByte() && p2 == 0x00.toByte()) {
            if (data.contentEquals(NDEF_TAG_AID)) {
                Log.d(TAG, "NDEF Tag AID selected")
                PaymentState.appendLog("NFC: Tag AID selected")
                return SW_OK
            }
        }
        
        // Select by File ID (P1=00, P2=0C)
        if (p1 == 0x00.toByte() && (p2 == 0x0C.toByte() || p2 == 0x00.toByte())) {
            if (data.size >= 2) {
                val fileId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                
                when (fileId.toShort()) {
                    CC_FILE_ID -> {
                        currentFile = CC_FILE_ID
                        Log.d(TAG, "CC file selected")
                        return SW_OK
                    }
                    NDEF_FILE_ID -> {
                        currentFile = NDEF_FILE_ID
                        // Prepare NDEF message when NDEF file is selected
                        prepareNdefMessage()
                        Log.d(TAG, "NDEF file selected")
                        return SW_OK
                    }
                }
            }
        }
        
        return SW_NOT_FOUND
    }
    
    private fun processReadBinary(apdu: ByteArray): ByteArray {
        if (apdu.size < 5) {
            return SW_ERROR
        }
        
        val offset = ((apdu[2].toInt() and 0xFF) shl 8) or (apdu[3].toInt() and 0xFF)
        val length = apdu[4].toInt() and 0xFF
        
        val fileData = when (currentFile) {
            CC_FILE_ID -> CC_FILE
            NDEF_FILE_ID -> ndefMessage ?: return SW_NOT_FOUND
            else -> return SW_NOT_FOUND
        }
        
        if (offset >= fileData.size) {
            return SW_ERROR
        }
        
        val endIndex = minOf(offset + length, fileData.size)
        val responseData = fileData.copyOfRange(offset, endIndex)
        
        Log.d(TAG, "Read binary: offset=$offset, length=$length, returning ${responseData.size} bytes")
        
        return responseData + SW_OK
    }
    
    private fun prepareNdefMessage() {
        // Get merchant phone from AppConfig
        val appConfig = AppConfig(applicationContext)
        val merchantPhone = appConfig.getMerchantPhone()
        
        // Get amount from PaymentState singleton
        val amount = PaymentState.currentAmount
        
        // If config is missing, return error
        if (merchantPhone.isBlank()) {
            Log.w(TAG, "Merchant phone not configured")
            ndefMessage = null
            return
        }
        
        // Use the current payment URI if available, otherwise construct USSD URI
        val uri = PaymentState.currentPaymentUri ?: run {
            if (amount != null && amount.isNotEmpty()) {
                "tel:*182*1*1*${merchantPhone}*${amount}#"
            } else {
                Log.w(TAG, "No payment amount available")
                null
            }
        }
        
        if (uri == null) {
            ndefMessage = null
            return
        }
        
        ndefMessage = createNdefMessage(uri)
        Log.d(TAG, "NDEF message prepared for URI: $uri")
        PaymentState.appendLog("NFC: Broadcasting URI")
    }
    
    /**
     * Creates an NDEF message with a URI record.
     * Format: [NLEN (2 bytes)][NDEF Record]
     * NDEF Record: [Header][Type Length][Payload Length][Type][Payload]
     */
    private fun createNdefMessage(uri: String): ByteArray {
        // For custom URI schemes, use identifier code 0x00 (no abbreviation)
        val uriBytes = uri.toByteArray(Charsets.UTF_8)
        val identifierCode: Byte = 0x00
        
        // NDEF Record header for URI:
        // TNF = 0x01 (Well Known), SR=1 (short record), ME=1, MB=1
        val header: Byte = 0xD1.toByte() // MB=1, ME=1, CF=0, SR=1, IL=0, TNF=001
        val typeLength: Byte = 0x01 // Type is "U" (1 byte)
        val payloadLength = (uriBytes.size + 1).toByte() // +1 for identifier code
        val type: Byte = 0x55 // 'U' for URI
        
        // Build NDEF record
        val ndefRecord = byteArrayOf(header, typeLength, payloadLength, type, identifierCode) + uriBytes
        
        // NLEN (2 bytes big-endian) + NDEF record
        val nlen = ndefRecord.size
        val nlenBytes = byteArrayOf(
            ((nlen shr 8) and 0xFF).toByte(),
            (nlen and 0xFF).toByte()
        )
        
        return nlenBytes + ndefRecord
    }
    
    override fun onDeactivated(reason: Int) {
        val reasonStr = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Link Lost"
            DEACTIVATION_DESELECTED -> "Deselected"
            else -> "Unknown"
        }
        Log.d(TAG, "Deactivated: $reasonStr")
        PaymentState.appendLog("NFC: $reasonStr")
        PaymentState.statusUpdate.postValue(reasonStr)
    }
}
