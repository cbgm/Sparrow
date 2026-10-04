package com.cbgm.sparrow.feature.notification.domain.model

sealed interface ConversationNotificationEvent {
    data class Show(
        val notification: ConversationNotification
    ) : ConversationNotificationEvent

    data class Cancel(
        val conversationId: String
    ) : ConversationNotificationEvent
}
