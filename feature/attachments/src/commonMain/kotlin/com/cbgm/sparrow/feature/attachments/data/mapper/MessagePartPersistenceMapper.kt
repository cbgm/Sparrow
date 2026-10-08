package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.messagepart.data.model.ContactDto
import com.cbgm.sparrow.core.messagepart.data.model.ExpenseBoardDto
import com.cbgm.sparrow.core.messagepart.data.model.ExpenseDto
import com.cbgm.sparrow.core.messagepart.data.model.FileDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.LocationDto
import com.cbgm.sparrow.core.messagepart.data.model.MessagePartDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.messagepart.data.model.VideoDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.data.database.entity.MessageBlobEntity
import com.cbgm.sparrow.data.database.entity.MessagePartEntity
import com.cbgm.sparrow.protocol.attachment.MessageAttachmentType
import kotlinx.serialization.json.Json

internal fun List<MessagePartDto>.flattenForPersistence(): List<MessagePartDto> =
    flatMap { part -> part.flattenForPersistence() }

private fun MessagePartDto.flattenForPersistence(): List<MessagePartDto> =
    when (this) {
        is PollDto -> listOf(this) + images
        is ExpenseDto -> listOfNotNull(this, receipt)
        else -> listOf(this)
    }

internal fun List<MessagePartDto>.withPersistedParts(parts: List<MessagePartDto>): List<MessagePartDto> {
    val partsById = parts.associateBy(MessagePartDto::id)
    return map { part -> part.withPersistedParts(partsById) }
}

private fun MessagePartDto.withPersistedParts(partsById: Map<String, MessagePartDto>): MessagePartDto =
    when (this) {
        is ExpenseDto -> copy(
            receipt = receipt?.let { image ->
                partsById[image.id] as? ImageDto
                    ?: error("Expense receipt ${image.id} was not persisted")
            }
        )
        is PollDto -> copy(
            images = images.map { image ->
                partsById[image.id] as? ImageDto
                    ?: error("Poll image ${image.id} was not persisted")
            }
        )

        else -> partsById[id] ?: this
    }

internal fun MessagePartDto.toMessagePartEntity(
    messageId: String,
    position: Int
): MessagePartEntity =
    MessagePartEntity(
        id = id,
        messageId = messageId,
        position = position,
        type = persistenceType(),
        payload = persistencePayload()
    )

private fun MessagePartDto.persistencePayload(): String? =
    when (this) {
        is PollDto -> Json.encodeToString(PollDto.serializer(), this)
        is ExpenseBoardDto -> Json.encodeToString(ExpenseBoardDto.serializer(), this)
        is ExpenseDto -> Json.encodeToString(ExpenseDto.serializer(), this)
        else -> null
    }

internal fun MessagePartDto.toMessageBlobEntityOrNull(
    deleteCapability: String?,
    localFilePath: String?
): MessageBlobEntity? =
    when (this) {
        is TextDto, is PollDto, is ExpenseDto, is ExpenseBoardDto -> null
        else -> toMessageBlobEntity(deleteCapability, localFilePath)
    }

internal fun MessagePartDto.toMessageBlobEntity(
    deleteCapability: String?,
    localFilePath: String?
): MessageBlobEntity {
    val reference = requireBlobReference()
    return MessageBlobEntity(
        partId = id,
        mimeType = mimeType(),
        byteSize = byteSize(),
        fileName = when (this) {
            is ImageDto -> fileName
            is VideoDto -> fileName
            is FileDto -> fileName
            else -> null
        },
        width = when (this) {
            is ImageDto -> width
            is VideoDto -> width
            else -> null
        },
        height = when (this) {
            is ImageDto -> height
            is VideoDto -> height
            else -> null
        },
        durationMilliseconds = when (this) {
            is VideoDto -> durationMilliseconds
            is VoiceDto -> durationMilliseconds
            else -> null
        },
        nodeId = reference.nodeId,
        blobId = reference.blobId,
        readCapability = reference.readCapability,
        ciphertextByteSize = reference.ciphertextByteSize,
        blobExpiresAtEpochMilliseconds = reference.expiresAtEpochMilliseconds,
        encryptionKey = reference.encryptionKey.copyOf(),
        nonce = reference.nonce.copyOf(),
        ciphertextSha256 = reference.ciphertextSha256.copyOf(),
        deleteCapability = deleteCapability,
        localFilePath = localFilePath
    )
}

internal fun MessagePartDto.requireBlobReference() =
    requireNotNull(
        when (this) {
            is ImageDto -> blob
            is VideoDto -> blob
            is FileDto -> blob
            is VoiceDto -> blob
            is LocationDto -> blob
            is ContactDto -> blob
            is TextDto -> null
            is PollDto, is ExpenseDto, is ExpenseBoardDto -> null
        }
    ) { "Message part $id has no blob reference" }

private fun MessagePartDto.persistenceType(): String =
    when (this) {
        is ImageDto -> MessageAttachmentType.IMAGE.name
        is VideoDto -> MessageAttachmentType.VIDEO.name
        is FileDto -> MessageAttachmentType.FILE.name
        is VoiceDto -> MessageAttachmentType.VOICE.name
        is LocationDto -> MessageAttachmentType.LOCATION.name
        is ContactDto -> MessageAttachmentType.CONTACT.name
        is TextDto -> error("Text is not persisted as an attachment message part")
        is PollDto -> MessageAttachmentType.POLL.name
        is ExpenseDto -> MessageAttachmentType.EXPENSE.name
        is ExpenseBoardDto -> MessageAttachmentType.EXPENSE_BOARD.name
    }

private fun MessagePartDto.mimeType(): String =
    when (this) {
        is ImageDto -> mimeType
        is VideoDto -> mimeType
        is FileDto -> mimeType
        is VoiceDto -> mimeType
        is LocationDto -> mimeType
        is ContactDto -> mimeType
        is TextDto -> error("Text has no attachment MIME type")
        is PollDto -> error("Poll has no attachment MIME type")
        is ExpenseDto, is ExpenseBoardDto -> error("Structured expenses have no attachment MIME type")
    }

private fun MessagePartDto.byteSize(): Long =
    when (this) {
        is ImageDto -> byteSize
        is VideoDto -> byteSize
        is FileDto -> byteSize
        is VoiceDto -> byteSize
        is LocationDto -> byteSize
        is ContactDto -> byteSize
        is TextDto -> error("Text has no attachment byte size")
        is PollDto -> error("Poll has no attachment byte size")
        is ExpenseDto, is ExpenseBoardDto -> error("Structured expenses have no attachment byte size")
    }
