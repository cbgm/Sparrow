package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi

suspend fun MediaSelectionUi.toOutgoingMessageAttachment(
    files: MediaSelectionFileRepository
): OutgoingMessageAttachment =
    when (this) {
        is VisualMediaSelectionUi ->
            OutgoingMessageAttachment(
                id = id,
                type =
                    when (type) {
                        MediaTypeUi.IMAGE -> MessageAttachmentType.IMAGE
                        MediaTypeUi.VIDEO -> MessageAttachmentType.VIDEO
                    },
                bytes = files.read(localFilePath),
                mimeType = mimeType,
                width = width,
                height = height,
                durationMilliseconds = durationMilliseconds
            )

        is FileMediaSelectionUi ->
            OutgoingMessageAttachment(
                id = id,
                type = MessageAttachmentType.FILE,
                bytes = files.read(localFilePath),
                mimeType = mimeType,
                fileName = fileName
            )
    }
