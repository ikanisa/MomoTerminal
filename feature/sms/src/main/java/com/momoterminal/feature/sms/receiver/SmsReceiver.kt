package com.momoterminal.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.momoterminal.sms.MomoSmsParser
import com.momoterminal.sms.VendorSmsProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * SMS receiver for Mobile Money messages.
 * 1. Catches incoming SMS
 * 2. Uses AI (OpenAI/Gemini) to parse transaction data
 * 3. Matches to registered vendor by MOMO number
 * 4. Saves to Supabase vendor_sms_transactions table
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject lateinit var smsParser: MomoSmsParser
    @Inject lateinit var vendorProcessor: VendorSmsProcessor
    
    // TODO: Get from secure config/environment variables
    private val AI_API_KEY = System.getenv("OPENAI_API_KEY") ?: ""
    private val USE_OPENAI = true // Set to false to use Gemini
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: continue
                val body = smsMessage.messageBody ?: continue
                val timestamp = smsMessage.timestampMillis
                
                Timber.d("SMS received from: $sender")
                
                // Check if it's a MoMo transaction
                if (smsParser.isMomoMessage(sender, body)) {
                    processMomoSms(sender, body, timestamp)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing SMS")
        }
    }
    
    private fun processMomoSms(sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.i("Processing MoMo SMS from $sender")
                
                // Process with AI and save to Supabase
                val result = vendorProcessor.processSms(
                    rawSms = body,
                    senderAddress = sender,
                    receivedAt = timestamp,
                    useOpenAI = USE_OPENAI,
                    apiKey = AI_API_KEY
                )
                
                result.onSuccess { transactionId ->
                    Timber.i("✅ SMS processed successfully: $transactionId")
                }.onFailure { error ->
                    Timber.e(error, "❌ Failed to process SMS")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process MoMo SMS")
            }
        }
    }
}
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
