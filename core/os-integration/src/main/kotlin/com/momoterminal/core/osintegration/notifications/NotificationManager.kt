package com.momoterminal.core.osintegration.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Generic notification channels
enum class NotificationChannelType(
    val id: String,
    val channelName: String,
    val importance: Int
) {
    GENERAL("general", "General", NotificationManager.IMPORTANCE_DEFAULT),
    UPDATES("updates", "Updates", NotificationManager.IMPORTANCE_LOW),
    REMINDERS("reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH),
    ALERTS("alerts", "Alerts", NotificationManager.IMPORTANCE_HIGH)
}

// Generic notification model
data class NotificationModel(
    val id: Int,
    val channelType: NotificationChannelType,
    val title: String,
    val message: String,
    val deepLink: String? = null, // e.g., "app://feature/item/123"
    val actions: List<NotificationAction> = emptyList(),
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
    val autoCancel: Boolean = true
)

data class NotificationAction(
    val id: String,
    val title: String,
    val deepLink: String
)

@Singleton
class AppNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelType.values().forEach { channelType ->
                val channel = NotificationChannel(
                    channelType.id,
                    channelType.channelName,
                    channelType.importance
                )
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun show(notification: NotificationModel) {
        val builder = NotificationCompat.Builder(context, notification.channelType.id)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(notification.priority)
            .setAutoCancel(notification.autoCancel)

        // Add deep link intent
        notification.deepLink?.let { deepLink ->
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(deepLink)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentIntent(pendingIntent)
        }

        // Add action buttons
        notification.actions.forEach { action ->
            val actionIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(action.deepLink))
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                action.id.hashCode(),
                actionIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, action.title, actionPendingIntent)
        }

        notificationManager.notify(notification.id, builder.build())
    }

    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }
}

// Usage example in a feature module
class NotificationExamples @Inject constructor(
    private val notificationManager: AppNotificationManager
) {
    fun showGenericUpdate() {
        notificationManager.show(
            NotificationModel(
                id = 1001,
                channelType = NotificationChannelType.UPDATES,
                title = "New Update Available",
                message = "Check out what's new",
                deepLink = "app://feature/updates"
            )
        )
    }

    fun showItemReminder(itemId: String, itemTitle: String) {
        notificationManager.show(
            NotificationModel(
                id = 2001,
                channelType = NotificationChannelType.REMINDERS,
                title = "Reminder",
                message = itemTitle,
                deepLink = "app://feature/item/$itemId",
                actions = listOf(
                    NotificationAction("view", "View", "app://feature/item/$itemId"),
                    NotificationAction("dismiss", "Dismiss", "app://dismiss")
                )
            )
        )
    }
}
