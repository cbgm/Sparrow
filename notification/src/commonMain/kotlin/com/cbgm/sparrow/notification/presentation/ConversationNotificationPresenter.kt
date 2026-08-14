package com.cbgm.sparrow.notification.presentation

import com.cbgm.sparrow.notification.model.ConversationNotification

interface ConversationNotificationPresenter {
    fun show(notification: ConversationNotification)

    fun cancel(conversationId: String)
}
