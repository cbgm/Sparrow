package com.cbgm.sparrow.feature.chats.domain.usecase.forward

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.chats.domain.model.ForwardMessageContent
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
                    .filterIsInstance<Text>()
                    .joinToString(separator = "\n", transform = Text::text)
            val attachments =
                coroutineScope {
                    parts
                        .filterNot { part -> part is Text }
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
            is Image ->
                OutgoingMessageAttachment.Image(
                    id = IdGenerator.generate(prefix = "image"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    width = requireNotNull(width),
                    height = requireNotNull(height)
                )

            is Video ->
                OutgoingMessageAttachment.Video(
                    id = IdGenerator.generate(prefix = "video"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    width = width,
                    height = height,
                    durationMilliseconds = durationMilliseconds
                )

            is File ->
                OutgoingMessageAttachment.File(
                    id = IdGenerator.generate(prefix = "file"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    fileName = fileName
                )

            is Location ->
                OutgoingMessageAttachment.Location(
                    id = IdGenerator.generate(prefix = "location"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow()
                )

            is Contact ->
                OutgoingMessageAttachment.Contact(
                    id = IdGenerator.generate(prefix = "contact"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow()
                )

            is Voice ->
                OutgoingMessageAttachment.Voice(
                    id = IdGenerator.generate(prefix = "voice"),
                    bytes = messageAttachmentRepository.loadBytes(id).getOrThrow(),
                    mimeType = mimeType,
                    durationMilliseconds = durationMilliseconds
                )

            is Poll -> error("Polls cannot be forwarded")
            is Text -> error("Text parts are forwarded as message text")
        }
}
