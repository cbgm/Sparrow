package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRow
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment

internal fun List<LocalMessageAttachmentRow>.toLocalAttachments(): List<LocalAttachment> =
    mapNotNull { row -> row.toLocalAttachment() }

internal fun List<LocalAttachment>.toAttachmentStorageSummary(
    conversationId: String,
    displayName: String,
    isGroup: Boolean
): AttachmentStorageSummary =
    AttachmentStorageSummary(
        conversationId = conversationId,
        displayName = displayName,
        isGroup = isGroup,
        mediaCount = count { attachment -> attachment.type != MessageAttachmentType.FILE },
        fileCount = count { attachment -> attachment.type == MessageAttachmentType.FILE },
        byteSize = sumOf(LocalAttachment::byteSize)
    )

private fun LocalMessageAttachmentRow.toLocalAttachment(): LocalAttachment? {
    val attachmentType = MessageAttachmentType.valueOf(attachment.type)
    if (attachmentType == MessageAttachmentType.LOCATION) return null

    return LocalAttachment(
        id = attachment.id,
        conversationId = conversationId,
        type = attachmentType,
        mimeType = attachment.mimeType,
        byteSize = attachment.byteSize,
        fileName = attachment.fileName,
        width = attachment.width,
        height = attachment.height,
        durationMilliseconds = attachment.durationMilliseconds,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )
}
