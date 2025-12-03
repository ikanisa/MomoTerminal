package com.momoterminal.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val smsPermissions = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )

    val phonePermissions = arrayOf(
        Manifest.permission.CALL_PHONE
    )

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else emptyArray()

    fun hasSmsPermissions(): Boolean = smsPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPhonePermissions(): Boolean = phonePermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun hasNfcSupport(): Boolean = NfcAdapter.getDefaultAdapter(context) != null

    fun isNfcEnabled(): Boolean = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    fun getAllRequiredPermissions(): Array<String> {
        return smsPermissions + phonePermissions + notificationPermission
    }

    fun getMissingPermissions(): List<String> {
        return getAllRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAllPermissions(): Boolean = getMissingPermissions().isEmpty()
}

object PermissionRationale {
    const val SMS_TITLE = "SMS Access Required"
    const val SMS_MESSAGE = """
MomoTerminal needs SMS access to:
• Read mobile money transaction confirmations
• Parse payment amounts and balances
• Update your wallet automatically

We ONLY read messages from mobile money providers.
Personal messages are never accessed or stored.
"""

    const val NFC_TITLE = "NFC Required"
    const val NFC_MESSAGE = """
MomoTerminal uses NFC to:
• Enable tap-to-pay functionality
• Read payment cards and tags
• Process contactless transactions

Please enable NFC in your device settings.
"""

    const val PHONE_TITLE = "Phone Access Required"
    const val PHONE_MESSAGE = """
MomoTerminal needs phone access to:
• Dial USSD codes for payments
• Complete mobile money transactions

This is required for the payment flow.
"""

    const val NOTIFICATION_TITLE = "Notifications"
    const val NOTIFICATION_MESSAGE = """
Enable notifications to:
• Receive payment confirmations
• Get transaction alerts
• Stay updated on sync status
"""
}
