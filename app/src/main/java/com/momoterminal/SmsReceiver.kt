package com.momoterminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.momoterminal.data.AppDatabase
import com.momoterminal.data.TransactionEntity
import com.momoterminal.sync.SyncManager
import com.momoterminal.webhook.WebhookDispatcher
import com.momoterminal.webhook.WebhookWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver that listens for incoming SMS messages
 * and saves Mobile Money related messages to the local database.
 * Uses offline-first approach: save immediately, then trigger sync.
 * Also dispatches SMS to configured webhooks for multi-endpoint delivery.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var webhookDispatcher: WebhookDispatcher
    
    companion object {
        private const val TAG = "SmsReceiver"
        
        // Keywords to filter Mobile Money messages
        private val MOMO_KEYWORDS = listOf("MOMO", "MobileMoney", "MTN", "RWF", "received", "sent", "payment")
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: "Unknown"
                val body = smsMessage.messageBody ?: ""
                val timestamp = smsMessage.timestampMillis
                
                Log.d(TAG, "SMS received from: $sender")
                
                // Log all messages for testing, but only save MOMO-related ones
                PaymentState.appendLog("SMS from $sender: ${body.take(50)}...")
                
                // Check if message is MOMO-related (case-insensitive)
                val isMomoMessage = MOMO_KEYWORDS.any { keyword ->
                    sender.contains(keyword, ignoreCase = true) ||
                    body.contains(keyword, ignoreCase = true)
                }
                
                if (isMomoMessage) {
                    // Save to local database immediately (offline-first)
                    saveToDatabase(context, sender, body, timestamp)
                    
                    // Trigger sync to upload to configured webhook
                    SyncManager(context).enqueueSyncNow()
                    
                    // Dispatch to all matching webhooks (multi-webhook relay)
                    dispatchToWebhooks(context, sender, body)
                    
                    PaymentState.appendLog("SMS saved and sync triggered")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            PaymentState.appendLog("SMS Error: ${e.message}")
        }
    }
    
    /**
     * Dispatch SMS to configured webhooks based on phone number matching.
     */
    private fun dispatchToWebhooks(context: Context, sender: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get the device's phone number (if available) for routing
                // For now, we use the sender as the identifier since we're receiving SMS
                val phoneNumber = getDevicePhoneNumber(context)
                
                webhookDispatcher.dispatchSms(
                    phoneNumber = phoneNumber,
                    sender = sender,
                    message = body
                )
                
                // Enqueue webhook worker to ensure delivery
                WebhookWorker.enqueueNow(context)
                
                Log.d(TAG, "SMS dispatched to webhooks")
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching to webhooks", e)
            }
        }
    }
    
    /**
     * Get the device phone number if available.
     * Falls back to empty string if not accessible.
     */
    private fun getDevicePhoneNumber(context: Context): String {
        // Note: Getting the phone number requires additional permissions
        // and may not always be available. The webhook routing should
        // also support matching by wildcard or empty phone number.
        return ""
    }
    
    private fun saveToDatabase(context: Context, sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val transaction = TransactionEntity(
                    sender = sender,
                    body = body,
                    timestamp = timestamp,
                    status = "PENDING"
                )
                database.transactionDao().insert(transaction)
                Log.d(TAG, "Transaction saved to database")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to database", e)
            }
        }
    }
}
