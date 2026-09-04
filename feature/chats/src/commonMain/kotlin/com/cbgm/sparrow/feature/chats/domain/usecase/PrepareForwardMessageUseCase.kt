package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.attachment.CONTACT_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.LOCATION_MIME_TYPE
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
import com.cbgm.sparrow.feature.chats.domain.model.ImageVideoType
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class PrepareForwardMessageUseCase(
    private val messageAttachmentRepository: MessageAttachmentRepository
) {
    suspend operator fun invoke(parts: List<MessagePart>): Result<ForwardMessageContent> =
        safeSuspendCall {
            val text =
                parts
                    .filterIsInstance<MessagePart.Text>()
                    .joinToString(separator = "\n", transform = MessagePart.Text::text)
            val attachments =
                coroutineScope {
                    parts
                        .filterNot { part -> part is MessagePart.Text }
                        .map { part -> async { part.toOutgoingAttachment() } }
                        .awaitAll()
                }

            require(text.isNotBlank() || attachments.isNotEmpty()) {
                "Message has no forwardable content"
            }

            ForwardMessageContent(
                text = text,
                attachments = attachments
            )
        }

    private suspend fun MessagePart.toOutgoingAttachment(): OutgoingMessageAttachment =
        when (this) {
            is MessagePart.ImageVideo -> {
                val attachmentType =
                    when (type) {
                        ImageVideoType.IMAGE -> MessageAttachmentType.IMAGE
                        ImageVideoType.VIDEO -> MessageAttachmentType.VIDEO
                    }
                OutgoingMessageAttachment(
                    id = IdGenerator.generate(prefix = attachmentType.name.lowercase()),
                    type = attachmentType,
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    width = width,
                    height = height,
                    durationMilliseconds = durationMilliseconds
                )
            }

            is MessagePart.File ->
                OutgoingMessageAttachment(
                    id = IdGenerator.generate(prefix = "file"),
                    type = MessageAttachmentType.FILE,
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    fileName = fileName
                )

            is MessagePart.Location ->
                OutgoingMessageAttachment(
                    id = IdGenerator.generate(prefix = "location"),
                    type = MessageAttachmentType.LOCATION,
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = LOCATION_MIME_TYPE
                )

            is MessagePart.Contact ->
                OutgoingMessageAttachment(
                    id = IdGenerator.generate(prefix = "contact"),
                    type = MessageAttachmentType.CONTACT,
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = CONTACT_MIME_TYPE
                )

            is MessagePart.Text ->
                error("Text parts are forwarded as message text")
        }
}
