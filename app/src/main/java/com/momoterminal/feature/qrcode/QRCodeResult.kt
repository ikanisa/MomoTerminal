package com.momoterminal.feature.qrcode

import com.google.mlkit.vision.barcode.common.Barcode

/**
 * Sealed class representing the result of a QR code scan operation.
 */
sealed class QRCodeResult {
    /**
     * Successful scan with parsed QR data.
     */
    data class Success(val data: ParsedQRData) : QRCodeResult()
    
    /**
     * No QR code found in the image.
     */
    data object NotFound : QRCodeResult()
    
    /**
     * Error occurred during scanning.
     */
    data class Error(val exception: Exception) : QRCodeResult()
}

/**
 * Sealed class representing different types of parsed QR code data.
 */
sealed class ParsedQRData {
    /**
     * Merchant payment QR code in MOMO://PAY format.
     * Example: MOMO://PAY?merchant=123456&amount=1000&ref=ORDER123
     */
    data class MerchantPayment(
        val merchantCode: String,
        val amount: Double?,
        val reference: String?,
        val merchantName: String?
    ) : ParsedQRData()
    
    /**
     * Peer-to-peer transfer QR code.
     */
    data class P2PTransfer(
        val phoneNumber: String,
        val amount: Double?,
        val name: String?
    ) : ParsedQRData()
    
    /**
     * URL QR code.
     */
    data class Url(val url: String) : ParsedQRData()
    
    /**
     * WiFi configuration QR code.
     */
    data class WiFi(
        val ssid: String,
        val password: String?,
        val encryptionType: String
    ) : ParsedQRData()
    
    /**
     * Contact information QR code.
     */
    data class Contact(
        val name: String?,
        val phone: String?,
        val email: String?,
        val organization: String?
    ) : ParsedQRData()
    
    /**
     * Plain text QR code.
     */
    data class Text(val text: String) : ParsedQRData()
}

/**
 * Parser object to convert ML Kit barcode results into structured data.
 */
object QRCodeParser {
    
    private const val MOMO_PAY_PREFIX = "MOMO://PAY"
    private const val MOMO_TRANSFER_PREFIX = "MOMO://TRANSFER"
    
    /**
     * Parse an ML Kit barcode into structured ParsedQRData.
     */
    fun parse(barcode: Barcode): ParsedQRData {
        val rawValue = barcode.rawValue ?: return ParsedQRData.Text("")
        
        // Check for MOMO payment format first
        if (rawValue.uppercase().startsWith(MOMO_PAY_PREFIX)) {
            return parseMomoPayment(rawValue)
        }
        
        if (rawValue.uppercase().startsWith(MOMO_TRANSFER_PREFIX)) {
            return parseMomoTransfer(rawValue)
        }
        
        // Use ML Kit's value type detection
        return when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                barcode.url?.let { url ->
                    ParsedQRData.Url(url.url ?: rawValue)
                } ?: ParsedQRData.Url(rawValue)
            }
            
            Barcode.TYPE_WIFI -> {
                barcode.wifi?.let { wifi ->
                    ParsedQRData.WiFi(
                        ssid = wifi.ssid ?: "",
                        password = wifi.password,
                        encryptionType = when (wifi.encryptionType) {
                            Barcode.WiFi.TYPE_WPA -> "WPA"
                            Barcode.WiFi.TYPE_WEP -> "WEP"
                            else -> "Open"
                        }
                    )
                } ?: ParsedQRData.Text(rawValue)
            }
            
            Barcode.TYPE_CONTACT_INFO -> {
                barcode.contactInfo?.let { contact ->
                    ParsedQRData.Contact(
                        name = contact.name?.formattedName,
                        phone = contact.phones.firstOrNull()?.number,
                        email = contact.emails.firstOrNull()?.address,
                        organization = contact.organization
                    )
                } ?: ParsedQRData.Text(rawValue)
            }
            
            Barcode.TYPE_PHONE -> {
                barcode.phone?.let { phone ->
                    ParsedQRData.P2PTransfer(
                        phoneNumber = phone.number ?: rawValue,
                        amount = null,
                        name = null
                    )
                } ?: ParsedQRData.Text(rawValue)
            }
            
            else -> ParsedQRData.Text(rawValue)
        }
    }
    
    /**
     * Parse MOMO://PAY format QR code.
     * Format: MOMO://PAY?merchant=CODE&amount=1000&ref=REF&name=NAME
     */
    private fun parseMomoPayment(rawValue: String): ParsedQRData {
        return try {
            val params = parseQueryParams(rawValue)
            ParsedQRData.MerchantPayment(
                merchantCode = params["merchant"] ?: params["m"] ?: "",
                amount = params["amount"]?.toDoubleOrNull() ?: params["a"]?.toDoubleOrNull(),
                reference = params["ref"] ?: params["r"],
                merchantName = params["name"] ?: params["n"]
            )
        } catch (e: Exception) {
            ParsedQRData.Text(rawValue)
        }
    }
    
    /**
     * Parse MOMO://TRANSFER format QR code.
     * Format: MOMO://TRANSFER?phone=1234567890&amount=1000&name=NAME
     */
    private fun parseMomoTransfer(rawValue: String): ParsedQRData {
        return try {
            val params = parseQueryParams(rawValue)
            ParsedQRData.P2PTransfer(
                phoneNumber = params["phone"] ?: params["p"] ?: "",
                amount = params["amount"]?.toDoubleOrNull() ?: params["a"]?.toDoubleOrNull(),
                name = params["name"] ?: params["n"]
            )
        } catch (e: Exception) {
            ParsedQRData.Text(rawValue)
        }
    }
    
    /**
     * Parse URL-style query parameters from a string.
     */
    private fun parseQueryParams(value: String): Map<String, String> {
        val queryStart = value.indexOf('?')
        if (queryStart == -1) return emptyMap()
        
        val query = value.substring(queryStart + 1)
        return query.split('&')
            .mapNotNull { param ->
                val parts = param.split('=', limit = 2)
                if (parts.size == 2) {
                    parts[0].lowercase() to java.net.URLDecoder.decode(parts[1], "UTF-8")
                } else {
                    null
                }
            }
            .toMap()
    }
}
