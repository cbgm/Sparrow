package com.cbgm.sparrow.data.database.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

data class ConversationSummaryDto(
    val conversationId: String,
    val contactId: String?,
    val contactName: String?,
    val conversationType: String,
    val conversationTitle: String?,
    val participantCount: Int,
    val lastMessageText: String?,
    val unreadCount: Int,
    val lastMessageTimestamp: Long?,
    val updatedAtEpochMilliseconds: Long,
    val lastMessageAttachmentType: MessageAttachmentType? = null
)
