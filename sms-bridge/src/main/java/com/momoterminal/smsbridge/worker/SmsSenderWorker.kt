package com.momoterminal.smsbridge.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.momoterminal.smsbridge.data.MessageDao
import com.momoterminal.smsbridge.data.MessageStatus
import com.momoterminal.smsbridge.data.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException

@HiltWorker
class SmsSenderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageDao: MessageDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient()
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val messageId = inputData.getString("messageId") ?: return Result.failure()
        
        val message = messageDao.getMessageById(messageId) ?: return Result.failure()
        
        // If message is already SENT, we are done
        if (message.status == MessageStatus.SENT) return Result.success()

        val webhookUrl = settingsRepository.webhookUrl.first()
        val secretToken = settingsRepository.secretToken.first()
        val deviceName = settingsRepository.deviceName.first()

        if (webhookUrl.isBlank() || !webhookUrl.startsWith("https://")) {
            updateStatus(messageId, MessageStatus.FAILED, 0, "Invalid Webhook URL")
            return Result.failure()
        }

        updateStatus(messageId, MessageStatus.SENDING, message.attempts + 1, null)

        val payload = mapOf(
            "type" to "sms",
            "messageId" to message.messageId,
            "from" to message.form,
            "body" to message.body,
            "receivedAt" to message.receivedAt,
            "deviceId" to message.deviceId, // In a real app this would be the actual IDs
            "deviceName" to deviceName,
            "simSlot" to message.simSlot
        )

        val json = gson.toJson(payload)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(webhookUrl)
            .post(body)
            .addHeader("X-SMSBRIDGE-SECRET", secretToken)
            .addHeader("X-SMSBRIDGE-DEVICE", message.deviceId)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                updateStatus(messageId, MessageStatus.SENT, message.attempts + 1, null)
                Result.success()
            } else {
                val code = response.code
                updateStatus(messageId, MessageStatus.FAILED, message.attempts + 1, "HTTP $code")
                // Failure for 5xx, permanent failure for 4xx?
                if (code in 400..499) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
        } catch (e: IOException) {
            updateStatus(messageId, MessageStatus.FAILED, message.attempts + 1, e.message)
            Result.retry()
        } catch (e: Exception) {
            updateStatus(messageId, MessageStatus.FAILED, message.attempts + 1, e.message)
            Result.failure()
        }
    }

    private suspend fun updateStatus(id: String, status: MessageStatus, attempts: Int, error: String?) {
        messageDao.updateStatus(id, status, attempts, error)
    }
}
