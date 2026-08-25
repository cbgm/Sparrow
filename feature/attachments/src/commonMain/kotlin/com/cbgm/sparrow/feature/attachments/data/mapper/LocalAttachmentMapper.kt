package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRow
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachmentType

internal fun List<LocalMessageAttachmentRow>.toLocalAttachments(): List<LocalAttachment> =
    map { row -> row.toLocalAttachment() }

internal fun List<LocalAttachment>.toAttachmentStorageSummary(
    conversationId: String,
    displayName: String,
    isGroup: Boolean
): AttachmentStorageSummary =
    AttachmentStorageSummary(
        conversationId = conversationId,
        displayName = displayName,
        isGroup = isGroup,
        mediaCount = count { attachment -> attachment.type != LocalAttachmentType.FILE },
        fileCount = count { attachment -> attachment.type == LocalAttachmentType.FILE },
        byteSize = sumOf(LocalAttachment::byteSize)
    )

private fun LocalMessageAttachmentRow.toLocalAttachment(): LocalAttachment =
    LocalAttachment(
        id = attachment.id,
        conversationId = conversationId,
        type = attachment.type.toLocalAttachmentType(),
        mimeType = attachment.mimeType,
        byteSize = attachment.byteSize,
        fileName = attachment.fileName,
        width = attachment.width,
        height = attachment.height,
        durationMilliseconds = attachment.durationMilliseconds,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )

private fun String.toLocalAttachmentType(): LocalAttachmentType =
    when (MessageAttachmentType.valueOf(this)) {
        MessageAttachmentType.IMAGE -> LocalAttachmentType.IMAGE
        MessageAttachmentType.VIDEO -> LocalAttachmentType.VIDEO
        MessageAttachmentType.FILE -> LocalAttachmentType.FILE
    }
