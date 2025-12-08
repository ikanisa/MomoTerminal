package com.momoterminal.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.momoterminal.ai.AiSmsParserService
import com.momoterminal.core.database.dao.SmsTransactionDao
import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SmsTransactionType
import com.momoterminal.core.database.entity.SyncStatus
import com.momoterminal.feature.sms.MomoSmsParser
import com.momoterminal.worker.SmsTransactionSyncWorker
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
    @Inject lateinit var aiSmsParserService: AiSmsParserService
    @Inject lateinit var smsTransactionDao: SmsTransactionDao
    
    companion object {
        private const val AI_CONFIDENCE_HIGH = 0.9f
        private const val REGEX_CONFIDENCE_DEFAULT = 0.7f
    }
    
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
                    processMomoSms(context, sender, body, timestamp)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing SMS")
        }
    }
    
    private fun processMomoSms(context: Context, sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.i("Processing MoMo SMS from $sender")
                
                // Parse SMS with AI parser (with regex fallback)
                val aiParsedData = aiSmsParserService.parseSmartly(sender, body)
                
                if (aiParsedData != null) {
                    Timber.i("✅ SMS parsed successfully: type=${aiParsedData.transactionType}, parsedBy=${aiParsedData.parsedBy}")
                    
                    // Convert AI parsed data to entity
                    val entity = SmsTransactionEntity(
                        rawMessage = body,
                        sender = sender,
                        amount = aiParsedData.getDisplayAmount(),
                        currency = aiParsedData.currency,
                        type = mapTransactionType(aiParsedData.transactionType),
                        balance = aiParsedData.getDisplayBalance(),
                        reference = aiParsedData.transactionId,
                        timestamp = timestamp,
                        synced = false,
                        syncStatus = SyncStatus.PENDING,
                        parsedBy = aiParsedData.parsedBy,
                        aiConfidence = if (aiParsedData.parsedBy == "gemini") AI_CONFIDENCE_HIGH else REGEX_CONFIDENCE_DEFAULT
                    )
                    
                    // Save to local database
                    smsTransactionDao.insert(entity)
                    Timber.i("SMS transaction saved to database: id=${entity.id}")
                    
                    // Schedule sync worker
                    scheduleSyncWorker(context)
                } else {
                    Timber.w("⚠️ Failed to parse SMS from $sender")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process MoMo SMS")
            }
        }
    }
    
    private fun scheduleSyncWorker(context: Context) {
        try {
            val syncWorkRequest = OneTimeWorkRequestBuilder<SmsTransactionSyncWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(syncWorkRequest)
            Timber.d("SmsTransactionSyncWorker scheduled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule sync worker")
        }
    }
    
    private fun mapTransactionType(type: String): SmsTransactionType {
        return when (type.uppercase()) {
            "RECEIVED" -> SmsTransactionType.RECEIVED
            "SENT" -> SmsTransactionType.SENT
            "CASH_OUT" -> SmsTransactionType.CASH_OUT
            "AIRTIME" -> SmsTransactionType.AIRTIME
            "DEPOSIT" -> SmsTransactionType.DEPOSIT
            else -> SmsTransactionType.UNKNOWN
        }
    }
}
