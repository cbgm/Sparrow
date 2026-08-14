package com.cbgm.securechat.notification.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

internal object SecureChatNotificationIntentFactory {
    fun createConversationIntent(
        context: Context,
        conversationId: String
    ): PendingIntent {
        val intent =
            requireNotNull(
                context.packageManager.getLaunchIntentForPackage(context.packageName)
            ) {
                "SecureChat launcher activity could not be resolved"
            }.apply {
                action = Intent.ACTION_VIEW
                data = SecureChatDeepLink.conversationUri(conversationId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
