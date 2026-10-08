package com.cbgm.sparrow.feature.notification.presentation

import com.cbgm.sparrow.feature.notification.domain.model.ConversationNotification

interface ConversationNotificationPresenter {
    fun show(notification: ConversationNotification)

    fun cancel(conversationId: String)
}
