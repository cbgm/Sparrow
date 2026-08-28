package com.cbgm.sparrow.notification.domain.model

data class PendingMessageSyncResult(
    val processedEnvelopeCount: Int,
    val notifications: List<ConversationNotification>
)
