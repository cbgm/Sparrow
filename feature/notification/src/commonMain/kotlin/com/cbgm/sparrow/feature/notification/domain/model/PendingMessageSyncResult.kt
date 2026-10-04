package com.cbgm.sparrow.feature.notification.domain.model

data class PendingMessageSyncResult(
    val processedEnvelopeCount: Int,
    val notifications: List<ConversationNotification>
)
