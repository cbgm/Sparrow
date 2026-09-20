package com.cbgm.sparrow.feature.attachments.data.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType

/** Data-layer transfer input. Validation of domain policy happens before mapping in the repository. */
data class OutgoingMessageAttachmentDto(
    val id: String,
    val type: MessageAttachmentType,
    val bytes: ByteArray,
    val mimeType: String,
    val fileName: String?,
    val width: Int?,
    val height: Int?,
    val durationMilliseconds: Long?
)
