package com.momoterminal.smsbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.momoterminal.smsbridge.data.AppDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: AppDatabase

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                // Re-enqueue pending messages
                val pending = database.messageDao().getPendingMessages()
                
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                pending.forEach { msg ->
                    val workRequest = OneTimeWorkRequestBuilder<com.momoterminal.smsbridge.worker.SmsSenderWorker>() // We need to import SenderWorker or use qualified name
                        .setConstraints(constraints)
                        .setInputData(Data.Builder().putString("messageId", msg.messageId).build())
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            10,
                            TimeUnit.SECONDS
                        )
                        .build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
