package com.momoterminal.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.momoterminal.core.common.auth.TokenManager
import com.momoterminal.core.common.config.AppConfig
import com.momoterminal.core.database.dao.TransactionDao
import com.momoterminal.core.database.entity.TransactionEntity
import com.momoterminal.feature.sms.SmsWalletIntegrationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that listens for incoming SMS messages
 * and saves Mobile Money related messages to the local database.
 * Integrates with wallet system for automatic crediting.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject lateinit var transactionDao: TransactionDao
    @Inject lateinit var smsWalletService: SmsWalletIntegrationService
    @Inject lateinit var tokenManager: TokenManager
    
    companion object {
        private const val TAG = "SmsReceiver"
        
        private val MOMO_KEYWORDS = listOf(
            "MOMO", "MobileMoney", "Mobile Money", "MTN", "Airtel", "Tigo", "Vodacom", "Halotel", "Lumicash", "EcoCash",
            "RWF", "CDF", "TZS", "BIF", "ZMW", "GHS", "USD",
            "received", "sent", "payment", "confirmed", "transferred"
        )
        
        const val BROADCAST_PAYMENT_RECEIVED = "com.momoterminal.action.PAYMENT_RECEIVED"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: "Unknown"
                val body = smsMessage.messageBody ?: ""
                val timestamp = smsMessage.timestampMillis
                
                Log.d(TAG, "SMS received from: $sender")
                
                val isMomoMessage = MOMO_KEYWORDS.any { keyword ->
                    sender.contains(keyword, ignoreCase = true) || body.contains(keyword, ignoreCase = true)
                }
                
                if (isMomoMessage) {
                    processAndSave(context, sender, body, timestamp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
    
    private fun processAndSave(context: Context, sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = tokenManager.getUserId()
                
                // Process through wallet integration (parses + credits wallet)
                if (userId != null) {
                    val result = smsWalletService.processIncomingSms(userId, sender, body)
                    Log.d(TAG, "SMS wallet processing result: $result")
                    
                    when (result) {
                        is SmsWalletIntegrationService.ProcessResult.CreditedWallet -> {
                            broadcastPaymentReceived(context, result.tokens, sender, result.smsId, timestamp)
                        }
                        else -> {}
                    }
                }
                
                // Save to transactions table
                val transaction = TransactionEntity(
                    sender = sender,
                    body = body,
                    timestamp = timestamp,
                    status = "PENDING",
                    amount = null, // TODO: Parse from SMS
                    currency = "RWF",
                    transactionId = null,
                    merchantCode = ""
                )
                transactionDao.insert(transaction)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to database", e)
            }
        }
    }
    
    private fun broadcastPaymentReceived(context: Context, amount: Long, sender: String, txnId: String, timestamp: Long) {
        val intent = Intent(BROADCAST_PAYMENT_RECEIVED).apply {
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_SENDER, sender)
            putExtra(EXTRA_TRANSACTION_ID, txnId)
            putExtra(EXTRA_TIMESTAMP, timestamp)
        }
        context.sendBroadcast(intent)
    }
}
