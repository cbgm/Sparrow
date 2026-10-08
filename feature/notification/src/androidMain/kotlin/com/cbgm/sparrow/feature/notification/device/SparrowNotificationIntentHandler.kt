package com.cbgm.sparrow.feature.notification.device

import android.content.Intent
import com.cbgm.sparrow.feature.notification.presentation.navigation.NotificationNavigationController

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
