package com.cbgm.securechat.notification.platform

import android.content.Intent
import com.cbgm.securechat.notification.navigation.NotificationNavigationController

class SecureChatNotificationIntentHandler(
    private val notificationNavigationController: NotificationNavigationController
) {
    fun handle(intent: Intent?): Boolean {
        val conversationId =
            SecureChatDeepLink.conversationId(intent)
                ?: return false

        notificationNavigationController.openConversation(
            conversationId = conversationId
        )

        return true
    }
}
