package com.momoterminal.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
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
 * 2. Uses AI (OpenAI/Gemini) to parse transaction data
 * 3. Matches to registered vendor by MOMO number
 * 4. Saves to Supabase vendor_sms_transactions table
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject lateinit var smsParser: MomoSmsParser
    
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
                
                // Parse SMS with MomoSmsParser
                val parsedData = smsParser.parse(sender, body)
                
                if (parsedData != null) {
                    Timber.i("✅ SMS parsed successfully: type=${parsedData.type}")
                    // TODO: Save to database or send to backend
                } else {
                    Timber.w("⚠️ Failed to parse SMS from $sender")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to process MoMo SMS")
            }
        }
    }
}
