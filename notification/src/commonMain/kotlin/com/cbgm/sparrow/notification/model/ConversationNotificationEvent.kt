package com.cbgm.sparrow.notification.model

sealed interface ConversationNotificationEvent {
    data class Show(
        val notification: ConversationNotification
    ) : ConversationNotificationEvent

    data class Cancel(
        val conversationId: String
    ) : ConversationNotificationEvent
}
