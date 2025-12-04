package com.momoterminal.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.momoterminal.core.database.dao.SmsTransactionDao
import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SmsTransactionType
import com.momoterminal.sms.MomoSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Simple SMS receiver for Mobile Money messages.
 * 1. Catches incoming SMS
 * 2. Parses MoMo transactions
 * 3. Saves to local database
 * 4. Syncs to Supabase (handled by background worker)
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject lateinit var smsParser: MomoSmsParser
    @Inject lateinit var smsDao: SmsTransactionDao
    
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
                // Parse the SMS
                val transaction = smsParser.parse(sender, body) ?: run {
                    Timber.w("Could not parse MoMo SMS from $sender")
                    return@launch
                }
                
                // Save to local database
                val entity = SmsTransactionEntity(
                    id = transaction?.transactionId ?: "",
                    sender = sender,
                    body = body,
                    amount = transaction?.amount?.toString() ?: "0",
                    currency = transaction?.currency ?: "",
                    transactionId = transaction?.transactionId ?: "",
                    type = SmsTransactionType.RECEIVED,
                    receivedAt = timestamp,
                    isSynced = false,
                    syncedAt = null,
                    createdAt = System.currentTimeMillis()
                )
                
                smsDao.insert(entity)
                Timber.i("Saved MoMo transaction: ${entity.transactionId} - ${entity.amount} ${entity.currency}")
                
                // Note: Background worker will sync to Supabase
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to save MoMo SMS")
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
