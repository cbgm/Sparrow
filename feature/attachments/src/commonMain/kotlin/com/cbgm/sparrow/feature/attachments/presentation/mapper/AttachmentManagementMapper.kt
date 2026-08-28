package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.presentation.model.MessageAttachmentUi
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload

internal fun List<LocalAttachment>.toAttachmentManagementUi(
    loadedBytes: Map<String, ByteArray>
): List<MessageAttachmentUi> =
    mapNotNull { attachment ->
        when (attachment.type) {
            MessageAttachmentType.IMAGE,
            MessageAttachmentType.VIDEO ->
                MessageAttachmentUi.ImageVideoAttachment(
                    id = attachment.id,
                    type = attachment.type,
                    mimeType = attachment.mimeType,
                    byteSize = attachment.byteSize,
                    fileName = attachment.fileName,
                    width = attachment.width,
                    height = attachment.height,
                    durationMilliseconds = attachment.durationMilliseconds,
                    bytes = loadedBytes[attachment.id]
                )

            MessageAttachmentType.FILE ->
                MessageAttachmentUi.FileAttachment(
                    id = attachment.id,
                    mimeType = attachment.mimeType,
                    byteSize = attachment.byteSize,
                    fileName = attachment.fileName ?: attachment.id
                )

            MessageAttachmentType.LOCATION ->
                loadedBytes[attachment.id]
                    ?.let(LocationAttachmentPayload::decode)
                    ?.let { location ->
                        MessageAttachmentUi.LocationAttachment(
                            id = attachment.id,
                            location = location
                        )
                    }
        }
    }
