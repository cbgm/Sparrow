package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentConstraints

object MessageAttachmentPolicy {
    const val MAX_ATTACHMENTS_PER_MESSAGE = MessageAttachmentConstraints.MAX_ATTACHMENTS_PER_MESSAGE
    const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    const val MAX_VIDEO_BYTES = 64L * 1024L * 1024L
    const val MAX_FILE_BYTES = 96L * 1024L * 1024L
    const val MAX_TOTAL_ATTACHMENT_BYTES = 96L * 1024L * 1024L
    const val MAX_IMAGE_DIMENSION = 2048
    const val DEFAULT_RETENTION_MILLISECONDS = 30L * 24L * 60L * 60L * 1_000L

    fun requireValid(attachments: List<OutgoingMessageAttachment>) {
        require(attachments.size <= MAX_ATTACHMENTS_PER_MESSAGE) {
            "A message can contain at most $MAX_ATTACHMENTS_PER_MESSAGE attachments"
        }
        require(attachments.map(OutgoingMessageAttachment::id).distinct().size == attachments.size) {
            "Attachment IDs must be unique"
        }
        require(attachments.sumOf { it.bytes.size.toLong() } <= MAX_TOTAL_ATTACHMENT_BYTES) {
            "Selected attachments exceed the total attachment size limit"
        }
    }
}
