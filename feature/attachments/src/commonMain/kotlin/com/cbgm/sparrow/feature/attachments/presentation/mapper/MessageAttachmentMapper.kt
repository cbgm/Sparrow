package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload

fun MessageAttachment.toUi(bytes: ByteArray? = null): MessageAttachmentUi? =
    when (type) {
        MessageAttachmentType.IMAGE,
        MessageAttachmentType.VIDEO ->
            MessageAttachmentUi.ImageVideoAttachment(
                id = id,
                type = type,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds,
                localFilePath = localFilePath,
                bytes = bytes
            )

        MessageAttachmentType.FILE ->
            MessageAttachmentUi.FileAttachment(
                id = id,
                mimeType = mimeType,
                byteSize = byteSize,
                fileName = fileName ?: id,
                localFilePath = localFilePath,
                bytes = bytes
            )

        MessageAttachmentType.LOCATION ->
            bytes
                ?.let(LocationAttachmentPayload::decode)
                ?.let { location ->
                    MessageAttachmentUi.LocationAttachment(
                        id = id,
                        location = location
                    )
                }
    }
