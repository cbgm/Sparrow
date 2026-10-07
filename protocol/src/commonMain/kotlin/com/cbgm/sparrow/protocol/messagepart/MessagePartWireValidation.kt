package com.cbgm.sparrow.protocol.messagepart

import com.cbgm.sparrow.core.messagepart.data.model.CONTACT_MIME_TYPE
import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy

internal fun List<MessagePartDto>.requireValidWireMessageParts(
    expectedTextPartId: String? = null
) {
    require(isNotEmpty()) { "Message must contain at least one message part" }
    require(map(MessagePartDto::id).distinct().size == size) {
        "Message part IDs must be unique within a message"
    }
    forEach { part ->
        require(part.id.isNotBlank()) { "Message part ID must not be blank" }
    }

    val textParts = filterIsInstance<TextDto>()
    require(textParts.size <= 1) { "A message can contain at most one text part" }
    textParts.singleOrNull()?.let { textPart ->
        require(textPart.text.isNotBlank()) { "Message text must not be blank" }
        if (expectedTextPartId != null) {
            require(textPart.id == expectedTextPartId) {
                "Text part ID must match the message ID"
            }
        }
    }

    val attachmentCount = count { part -> part !is TextDto }
    require(attachmentCount <= MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE) {
        "A message can contain at most ${MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE} attachments"
    }

    val voiceCount = count { part -> part is VoiceDto }
    require(voiceCount == 0 || (voiceCount == 1 && size == 1)) {
        "A voice message cannot contain text or other attachments"
    }

    forEach { part ->
        when (part) {
            is TextDto -> Unit
            is ImageDto -> {
                val width = part.width
                val height = part.height

                requireNotNull(part.blob) { "Image message part requires a blob reference" }
                require(part.mimeType.startsWith("image/")) { "Image message part must use an image MIME type" }
                require(part.byteSize > 0L) { "Image message part byte size must be positive" }
                require(width == null || width > 0) { "Image width must be positive" }
                require(height == null || height > 0) { "Image height must be positive" }
            }
            is VideoDto -> {
                val width = part.width
                val height = part.height
                val durationMilliseconds = part.durationMilliseconds

                requireNotNull(part.blob) { "Video message part requires a blob reference" }
                require(part.mimeType.startsWith("video/")) { "Video message part must use a video MIME type" }
                require(part.byteSize > 0L) { "Video message part byte size must be positive" }
                require((width == null) == (height == null)) {
                    "Video dimensions must be both present or both absent"
                }
                require(width == null || width > 0) { "Video width must be positive" }
                require(height == null || height > 0) { "Video height must be positive" }
                require(durationMilliseconds == null || durationMilliseconds >= 0L) {
                    "Video duration must not be negative"
                }
            }
            is FileDto -> {
                requireNotNull(part.blob) { "File message part requires a blob reference" }
                require(part.mimeType.isNotBlank()) { "File MIME type must not be blank" }
                require(part.byteSize > 0L) { "File message part byte size must be positive" }
                require(part.fileName.isNotBlank()) { "File name must not be blank" }
            }
            is VoiceDto -> {
                requireNotNull(part.blob) { "Voice message part requires a blob reference" }
                require(part.mimeType.startsWith("audio/")) { "Voice message part must use an audio MIME type" }
                require(part.byteSize > 0L) { "Voice message part byte size must be positive" }
                require(part.durationMilliseconds >= 0L) { "Voice duration must not be negative" }
            }
            is LocationDto -> {
                requireNotNull(part.blob) { "Location message part requires a blob reference" }
                require(part.mimeType == LOCATION_MIME_TYPE) {
                    "Location message part must use the Sparrow location MIME type"
                }
                require(part.byteSize > 0L) { "Location message part byte size must be positive" }
            }
            is ContactDto -> {
                requireNotNull(part.blob) { "Contact message part requires a blob reference" }
                require(part.mimeType == CONTACT_MIME_TYPE) {
                    "Contact message part must use the Sparrow contact MIME type"
                }
                require(part.byteSize > 0L) { "Contact message part byte size must be positive" }
            }
            is PollDto -> error("Poll message-part transport is not wired yet")
        }
    }
}
