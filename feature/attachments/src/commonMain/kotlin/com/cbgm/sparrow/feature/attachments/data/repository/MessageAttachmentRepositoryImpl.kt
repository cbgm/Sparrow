package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.data.datasource.AttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentContentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.LocalAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.mapper.toAttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.data.mapper.toAttachmentTranscript
import com.cbgm.sparrow.feature.attachments.data.mapper.toDomain
import com.cbgm.sparrow.feature.attachments.data.mapper.toDto
import com.cbgm.sparrow.feature.attachments.data.mapper.toLocalAttachments
import com.cbgm.sparrow.feature.attachments.data.mapper.toPersistedTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentStorageSummary
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTranscript
import com.cbgm.sparrow.feature.attachments.domain.model.LocalAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MessageAttachmentRepositoryImpl(
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val localAttachmentDataSource: LocalAttachmentDataSource,
    private val attachmentContentDataSource: AttachmentContentDataSource,
    private val localAttachmentContentDataSource: LocalAttachmentContentDataSource
) : MessageAttachmentRepository {
    private val logger = SparrowLog.withTag("MessageAttachmentRepository")

    override suspend fun loadContent(target: AttachmentTarget): Result<AttachmentContent> =
        safeSuspendCall {
            val key = target.toDto()
            val payload = localAttachmentContentDataSource.get(key)
                ?: attachmentContentDataSource.load(key).also { content ->
                    localAttachmentContentDataSource.save(key, content)
                }
            payload.toDomain(target)
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
            attachmentContentDataSource.loadBytes(target.toDto())
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
            .map { value -> value?.toAttachmentTranscript() }

    override fun observeLocalAttachments(conversationId: String): Flow<List<LocalAttachment>> =
        localAttachmentDataSource.observeByConversation(conversationId)
            .map { rows -> rows.toLocalAttachments() }

    override fun observeStorageSummaries(): Flow<List<AttachmentStorageSummary>> =
        localAttachmentDataSource.observeStorageSummaries()
            .map { summaries ->
                summaries.map { summary ->
                    summary.rows.toLocalAttachments().toAttachmentStorageSummary(
                        conversationId = summary.conversationId,
                        displayName = summary.displayName,
                        isGroup = summary.isGroup
                    )
                }
            }

    override suspend fun deleteLocalAttachments(attachmentIds: Set<String>): Result<Unit> = safeSuspendCall {
        localAttachmentDataSource.delete(attachmentIds)
        localAttachmentContentDataSource.removeAttachmentIds(attachmentIds)
    }

    override suspend fun deleteLocalAttachmentsForConversation(conversationId: String): Result<Unit> = safeSuspendCall {
        localAttachmentDataSource.deleteForConversation(conversationId)
        localAttachmentContentDataSource.clear()
    }
}
