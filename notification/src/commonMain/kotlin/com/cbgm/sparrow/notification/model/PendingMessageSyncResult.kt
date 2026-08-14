package com.cbgm.sparrow.notification.model

data class PendingMessageSyncResult(
    val processedEnvelopeCount: Int,
    val notifications: List<ConversationNotification>
)
