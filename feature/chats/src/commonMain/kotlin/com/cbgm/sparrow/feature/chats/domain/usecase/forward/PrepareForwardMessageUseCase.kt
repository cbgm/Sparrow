package com.cbgm.sparrow.feature.chats.domain.usecase.forward

import com.cbgm.sparrow.core.id.IdGenerator
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
                val bytes = messageAttachmentRepository.loadBytes(id).getOrThrow()
                when (type) {
                    ImageVideoType.IMAGE ->
                        OutgoingMessageAttachment.Image(
                            id = IdGenerator.generate(prefix = "image"),
                            bytes = bytes,
                            mimeType = mimeType,
                            width = requireNotNull(width),
                            height = requireNotNull(height)
                        )

                    ImageVideoType.VIDEO ->
                        OutgoingMessageAttachment.Video(
                            id = IdGenerator.generate(prefix = "video"),
                            bytes = bytes,
                            mimeType = mimeType,
                            width = width,
                            height = height,
                            durationMilliseconds = durationMilliseconds
                        )
                }
            }

            is MessagePart.File ->
                OutgoingMessageAttachment.File(
                    id = IdGenerator.generate(prefix = "file"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    fileName = fileName
                )

            is MessagePart.Location ->
                OutgoingMessageAttachment.Location(
                    id = IdGenerator.generate(prefix = "location"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow()
                )

            is MessagePart.Contact ->
                OutgoingMessageAttachment.Contact(
                    id = IdGenerator.generate(prefix = "contact"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow()
                )

            is MessagePart.Voice ->
                OutgoingMessageAttachment.Voice(
                    id = IdGenerator.generate(prefix = "voice"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    durationMilliseconds = durationMilliseconds
                )

            is MessagePart.Text -> error("Text parts are forwarded as message text")
        }
}
