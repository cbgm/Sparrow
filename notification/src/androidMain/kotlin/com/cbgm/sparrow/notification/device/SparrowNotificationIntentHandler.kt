package com.cbgm.sparrow.notification.device

import android.content.Intent
import com.cbgm.sparrow.notification.presentation.navigation.NotificationNavigationController

class SparrowNotificationIntentHandler(
    private val notificationNavigationController: NotificationNavigationController
) {
    fun handle(intent: Intent?): Boolean {
        val conversationId =
            SparrowDeepLink.conversationId(intent)
                ?: return false

        notificationNavigationController.openConversation(
            conversationId = conversationId
        )

        return true
    }
}
