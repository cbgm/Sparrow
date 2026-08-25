package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.MessageMediaType
import com.cbgm.sparrow.feature.attachments.presentation.management.model.AttachmentFileUi
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageMediaAttachmentUi

internal fun List<LocalAttachment>.toAttachmentManagementMediaModels(
    loadedBytes: Map<String, ByteArray>
): List<MessageMediaAttachmentUi> =
    asSequence()
        .filter { attachment -> attachment.type != LocalAttachmentType.FILE }
        .map { attachment ->
            MessageMediaAttachmentUi(
                id = attachment.id,
                type =
                    when (attachment.type) {
                        LocalAttachmentType.IMAGE -> MessageMediaType.IMAGE
                        LocalAttachmentType.VIDEO -> MessageMediaType.VIDEO
                        LocalAttachmentType.FILE -> error("File attachment cannot be mapped to media")
                    },
                mimeType = attachment.mimeType,
                width = attachment.width,
                height = attachment.height,
                durationMilliseconds = attachment.durationMilliseconds,
                bytes = loadedBytes[attachment.id]
            )
        }.toList()

internal fun List<LocalAttachment>.toAttachmentManagementFileModels(): List<AttachmentFileUi> =
    asSequence()
        .filter { attachment -> attachment.type == LocalAttachmentType.FILE }
        .map { attachment ->
            AttachmentFileUi(
                id = attachment.id,
                displayName = attachment.fileName ?: attachment.id,
                sizeText = attachment.byteSize.toReadableByteSize()
            )
        }.toList()

private fun Long.toReadableByteSize(): String =
    when {
        this >= BYTES_PER_MEGABYTE -> "${this / BYTES_PER_MEGABYTE} MB"
        this >= BYTES_PER_KILOBYTE -> "${this / BYTES_PER_KILOBYTE} KB"
        else -> "$this B"
    }

private const val BYTES_PER_KILOBYTE = 1024L
private const val BYTES_PER_MEGABYTE = BYTES_PER_KILOBYTE * 1024L
