package com.cbgm.securechat.notification.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cbgm.securechat.notification.model.ConversationNotification
import com.cbgm.securechat.notification.presentation.ConversationNotificationPresenter
import com.cbgm.securechat.resources.R as ResourcesR

class SecureChatNotificationManager(
    private val context: Context
) : ConversationNotificationPresenter {
    fun createChannels() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                MESSAGE_CHANNEL_ID,
                context.getString(ResourcesR.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(ResourcesR.string.notification_channel_messages_description)
            }
        )
    }

    @SuppressLint("MissingPermission")
    override fun show(notification: ConversationNotification) {
        createChannels()
        if (!canPostNotifications()) {
            return
        }

        val title =
            notification.title.takeIf(String::isNotBlank)
                ?: context.getString(ResourcesR.string.app_name)
        val preview =
            notification.messagePreview
                ?: context.getString(ResourcesR.string.notification_new_message)

        val androidNotification =
            NotificationCompat
                .Builder(context, MESSAGE_CHANNEL_ID)
                .setSmallIcon(ResourcesR.drawable.ic_securechat_notification)
                .setContentTitle(title)
                .setContentText(preview)
                .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
                .setContentIntent(
                    SecureChatNotificationIntentFactory.createConversationIntent(
                        context = context,
                        conversationId = notification.conversationId
                    )
                ).setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setNumber(notification.unreadCount)
                .setGroup(MESSAGE_GROUP_KEY)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                notification.conversationId.hashCode(),
                androidNotification
            )

        cancelWakeUpNotification()
    }

    override fun cancel(conversationId: String) {
        NotificationManagerCompat
            .from(context)
            .cancel(conversationId.hashCode())
    }

    private fun cancelWakeUpNotification() {
        NotificationManagerCompat
            .from(context)
            .cancel(WAKE_UP_NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val WAKE_UP_NOTIFICATION_ID = 10_001
        const val MESSAGE_CHANNEL_ID = "securechat-messages"
        const val MESSAGE_GROUP_KEY = "securechat-message-notifications"
    }
}
