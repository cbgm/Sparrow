package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.data.datasource.AttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.mapper.toPersistedTranscript
import com.cbgm.sparrow.feature.attachments.data.model.AttachmentContentPayloadDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload
import kotlinx.coroutines.flow.Flow

internal class MessageAttachmentRepositoryImpl(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource,
    private val attachmentContentDataSource: AttachmentContentDataSource,
    private val localAttachmentContentDataSource: LocalAttachmentContentDataSource
) : MessageAttachmentRepository {
    private val logger = SparrowLog.withTag("MessageAttachmentRepository")

    override suspend fun loadContent(target: AttachmentTarget): Result<AttachmentContent> =
        safeSuspendCall {
            localAttachmentContentDataSource.get(target)
                ?: attachmentContentDataSource
                    .load(target)
                    .toDomain(target)
                    .also { content -> localAttachmentContentDataSource.save(content) }
        }.onFailure { error ->
            logger.error(error) { "Could not load attachment content ${target.id}" }
        }

    override suspend fun loadBytes(attachmentId: String): Result<ByteArray> =
        safeSuspendCall {
            messageAttachmentDataSource.loadBytes(attachmentId)
        }.onFailure { error ->
            logger.error(error) { "Could not load attachment bytes $attachmentId" }
        }

    override suspend fun loadBytes(target: AttachmentTarget): Result<ByteArray> =
        safeSuspendCall {
            attachmentContentDataSource.loadBytes(target)
        }.onFailure { error ->
            logger.error(error) { "Could not load attachment bytes ${target.id}" }
        }

    override suspend fun saveTranscript(
        attachmentId: String,
        transcription: AttachmentTranscript
    ): Result<Unit> =
        safeSuspendCall {
            messageAttachmentDataSource.updateTranscript(
                attachmentId = attachmentId,
                transcript = transcription.toPersistedTranscript()
            )
        }.onFailure { error ->
            logger.error(error) { "Could not save attachment transcript $attachmentId" }
        }

    override fun observeTranscript(attachmentId: String): Flow<AttachmentTranscript?> =
        messageAttachmentDataSource.observeTranscript(attachmentId)

    override fun observeLocalAttachments(conversationId: String): Flow<List<LocalAttachment>> =
        localAttachmentDataSource.observeByConversation(conversationId)

    override fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>> =
        localAttachmentDataSource.observeStorageSummaries()

    override suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit> = safeSuspendCall {
        localAttachmentDataSource.delete(attachmentIds)
        localAttachmentContentDataSource.removeAttachmentIds(attachmentIds)
    }

    override suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit> = safeSuspendCall {
        localAttachmentDataSource.deleteForConversation(conversationId)
        localAttachmentContentDataSource.clear()
    }

    private fun AttachmentContentPayloadDto.toDomain(target: AttachmentTarget): AttachmentContent =
        when (this) {
            is AttachmentContentPayloadDto.LocalFile ->
                AttachmentContent.LocalFile(
                    target = target,
                    localFilePath = localFilePath
                )

            is AttachmentContentPayloadDto.Payload ->
                when (target.type) {
                    MessageAttachmentType.LOCATION ->
                        AttachmentContent.Location(
                            target = target,
                            location =
                                requireNotNull(LocationAttachmentPayload.decode(bytes)) {
                                    "Location attachment payload is invalid"
                                }
                        )

                    MessageAttachmentType.CONTACT ->
                        AttachmentContent.Contact(
                            target = target,
                            contact =
                                requireNotNull(ContactAttachmentPayload.decode(bytes)) {
                                    "Contact attachment payload is invalid"
                                }
                        )

                    MessageAttachmentType.IMAGE,
                    MessageAttachmentType.VIDEO,
                    MessageAttachmentType.FILE,
                    MessageAttachmentType.VOICE ->
                        error("Unexpected binary payload for attachment type ${target.type}")
                }
        }
}
