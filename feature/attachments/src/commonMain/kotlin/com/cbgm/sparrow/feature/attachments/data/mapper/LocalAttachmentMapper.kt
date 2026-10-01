package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.data.database.model.LocalMessageAttachmentRowDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType

internal fun List<LocalMessageAttachmentRowDto>.toLocalAttachments(): List<LocalAttachment> =
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
        mediaCount = count { attachment -> attachment !is LocalAttachment.File },
        fileCount = count { attachment -> attachment is LocalAttachment.File },
        byteSize = sumOf(LocalAttachment::byteSize)
    )

private fun LocalMessageAttachmentRowDto.toLocalAttachment(): LocalAttachment? {
    val type = MessageAttachmentType.valueOf(attachment.type)
    return when (type) {
        MessageAttachmentType.IMAGE,
        MessageAttachmentType.VIDEO ->
            LocalAttachment.Media(
                id = attachment.id,
                conversationId = conversationId,
                mimeType = attachment.mimeType,
                byteSize = attachment.byteSize,
                mediaType = type.toMediaContentType(),
                fileName = attachment.fileName,
                width = attachment.width,
                height = attachment.height,
                durationMilliseconds = attachment.durationMilliseconds,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds
            )

        MessageAttachmentType.FILE ->
            LocalAttachment.File(
                id = attachment.id,
                conversationId = conversationId,
                mimeType = attachment.mimeType,
                byteSize = attachment.byteSize,
                fileName = attachment.fileName,
                createdAtEpochMilliseconds = createdAtEpochMilliseconds
            )

        MessageAttachmentType.VOICE ->
            LocalAttachment.Voice(
                id = attachment.id,
                conversationId = conversationId,
                mimeType = attachment.mimeType,
                byteSize = attachment.byteSize,
                durationMilliseconds = requireNotNull(attachment.durationMilliseconds) {
                    "Voice attachment ${attachment.id} is missing duration"
                },
                createdAtEpochMilliseconds = createdAtEpochMilliseconds
            )

        MessageAttachmentType.LOCATION,
        MessageAttachmentType.CONTACT -> null
    }
}

private fun MessageAttachmentType.toMediaContentType(): MediaContentType =
    when (this) {
        MessageAttachmentType.IMAGE -> MediaContentType.IMAGE
        MessageAttachmentType.VIDEO -> MediaContentType.VIDEO
        else -> error("Attachment type $this is not media")
    }
