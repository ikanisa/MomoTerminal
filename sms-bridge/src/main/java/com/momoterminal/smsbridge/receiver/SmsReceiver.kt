package com.momoterminal.smsbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.momoterminal.smsbridge.data.AppDatabase
import com.momoterminal.smsbridge.data.MessageEntity
import com.momoterminal.smsbridge.data.SettingsRepository
import com.momoterminal.smsbridge.worker.SmsSenderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                // Check if forwarding is enabled
                if (!settingsRepository.isForwardingEnabled.first()) {
                    Timber.d("Forwarding disabled, ignoring SMS")
                    return@launch
                }
                
                // Get device name
                val devName = settingsRepository.deviceName.first()

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return@launch

                // Group by sender to handle multipart if needed, though getMessages returns array of parts
                // Usually we treat the whole intent as one message entity if they are concatenated.
                // However, Android 8+ might handle concatenation? 
                // Simple approach: Combine body of all parts from same sender. 
                // But usually Broadcast contains parts of ONE message.
                
                val sb = StringBuilder()
                var sender = ""
                var slot = 0
                
                // Try to get sim slot if possible (extra "slot" or "phone", varies by OEM)
                slot = intent.getIntExtra("slot", -1)
                
                for (sms in messages) {
                    if (sms == null) continue
                    sb.append(sms.messageBody)
                    sender = sms.originatingAddress ?: "Unknown"
                }
                
                val body = sb.toString()
                
                // Create unique ID
                val msgId = UUID.randomUUID().toString()
                
                // Device ID (ANDROID_ID)
                val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
                val installId = "install-uuid" // Should generate and store this once, simplifying for now
                val fullDeviceId = "androidId:$androidId"

                val receivedTime = try {
                    ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                } catch (e: Exception) {
                    System.currentTimeMillis().toString()
                }

                val entity = MessageEntity(
                    messageId = msgId,
                    form = sender,
                    body = body,
                    receivedAt = receivedTime,
                    deviceId = fullDeviceId,
                    deviceName = devName,
                    simSlot = slot
                )
                
                database.messageDao().insertMessage(entity)
                
                // Enqueue Work
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                
                val workRequest = OneTimeWorkRequestBuilder<SmsSenderWorker>()
                    .setConstraints(constraints)
                    .setInputData(Data.Builder().putString("messageId", msgId).build())
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        10,
                        TimeUnit.SECONDS
                    )
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
                
            } catch (e: Exception) {
                Timber.e(e, "Error processing SMS")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
