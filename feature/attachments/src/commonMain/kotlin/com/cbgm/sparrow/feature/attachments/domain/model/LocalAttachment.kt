package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

data class LocalAttachment(
    val id: String,
    val conversationId: String,
    val type: MessageAttachmentType,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String?,
    val width: Int?,
    val height: Int?,
    val durationMilliseconds: Long?,
    val createdAtEpochMilliseconds: Long
)
