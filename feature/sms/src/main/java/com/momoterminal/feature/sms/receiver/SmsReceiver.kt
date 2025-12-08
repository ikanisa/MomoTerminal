package com.momoterminal.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.momoterminal.core.ai.AiParserChain
import com.momoterminal.core.common.worker.SmsTransactionSyncScheduler
import com.momoterminal.core.database.dao.SmsTransactionDao
import com.momoterminal.core.database.entity.SmsTransactionEntity
import com.momoterminal.core.database.entity.SmsTransactionType
import com.momoterminal.core.database.entity.SyncStatus
import com.momoterminal.feature.sms.MomoSmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * SMS receiver for Mobile Money messages.
 * 1. Catches incoming SMS
 * 2. Uses AI (OpenAI PRIMARY → Gemini FALLBACK → Regex) to parse transaction data
 * 3. Matches to registered vendor by MOMO number
 * 4. Saves to Supabase vendor_sms_transactions table
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject lateinit var smsParser: MomoSmsParser
    @Inject lateinit var aiParserChain: AiParserChain
    @Inject lateinit var smsTransactionDao: SmsTransactionDao
    @Inject lateinit var syncScheduler: SmsTransactionSyncScheduler
    
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
                
                // Parse SMS with AI parser chain (OpenAI → Gemini → Regex)
                val parseResult = aiParserChain.parse(sender, body)
                
                Timber.i("✅ SMS parsed successfully: type=${parseResult.entity.type}, parsedBy=${parseResult.parsedBy}, confidence=${parseResult.confidence}")
                
                // Update timestamp
                val entity = parseResult.entity.copy(
                    timestamp = timestamp
                )
                
                // Save to local database
                smsTransactionDao.insert(entity)
                Timber.i("SMS transaction saved to database: id=${entity.id}")
                
                // Schedule sync worker to upload to Supabase
                syncScheduler.scheduleSync(context)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process MoMo SMS")
            }
        }
    }
}
