package com.cbgm.sparrow.feature.attachments.data.model

internal data class AttachmentMessageContextDto(
    val conversationId: String,
    val createdAtEpochMilliseconds: Long,
    val displayName: String,
    val isGroup: Boolean,
    val isMine: Boolean,
    val senderContactId: String?
)
