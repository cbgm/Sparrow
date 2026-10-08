package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.data.datasource.AttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.mapper.toAttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.data.mapper.toAttachmentTranscript
import com.cbgm.sparrow.feature.attachments.data.mapper.toPersistedTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.attachments.util.ContactAttachmentPayload
import com.cbgm.sparrow.feature.attachments.util.LocationAttachmentPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MessageAttachmentRepositoryImpl(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource,
    private val attachmentContentDataSource: AttachmentContentDataSource
) : MessageAttachmentRepository {
    private val logger = SparrowLog.withTag("MessageAttachmentRepository")

    override suspend fun loadLocalFile(partId: String, groupId: String?): Result<String> =
        safeSuspendCall {
            attachmentContentDataSource.loadLocalFile(partId, groupId)
        }.onFailure { error ->
            logger.error(error) { "Could not load local message-part content $partId" }
        }

    override suspend fun loadLocation(partId: String, groupId: String?): Result<CurrentLocation> =
        safeSuspendCall {
            val bytes = attachmentContentDataSource.loadBytes(partId, groupId)
            requireNotNull(LocationAttachmentPayload.decode(bytes)) {
                "Location message-part payload is invalid"
            }
        }.onFailure { error ->
            logger.error(error) { "Could not load location message part $partId" }
        }

    override suspend fun loadContact(partId: String, groupId: String?): Result<SharedContact> =
        safeSuspendCall {
            val bytes = attachmentContentDataSource.loadBytes(partId, groupId)
            requireNotNull(ContactAttachmentPayload.decode(bytes)) {
                "Contact message-part payload is invalid"
            }
        }.onFailure { error ->
            logger.error(error) { "Could not load contact message part $partId" }
        }

    override suspend fun loadBytes(partId: String, groupId: String?): Result<ByteArray> =
        safeSuspendCall {
            attachmentContentDataSource.loadBytes(partId, groupId)
        }.onFailure { error ->
            logger.error(error) { "Could not load message-part bytes $partId" }
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
            .map { value -> value?.toAttachmentTranscript() }

    override fun observeLocalAttachments(conversationId: String): Flow<List<MessagePart>> =
        localAttachmentDataSource.observeByConversation(conversationId)
            .map { parts -> parts.map { part -> part.toMessagePart() } }

    override fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>> =
        localAttachmentDataSource.observeStorageSummaries()
            .map { summaries -> summaries.map { summary -> summary.toAttachmentStorageSummary() } }

    override suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit> = safeSuspendCall {
        localAttachmentDataSource.delete(attachmentIds)
    }

    override suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit> = safeSuspendCall {
        localAttachmentDataSource.deleteForConversation(conversationId)
    }
}
