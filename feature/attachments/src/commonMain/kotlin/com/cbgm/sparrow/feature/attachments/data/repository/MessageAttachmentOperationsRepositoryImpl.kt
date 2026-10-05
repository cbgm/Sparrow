package com.cbgm.sparrow.feature.attachments.data.repository

import com.cbgm.sparrow.core.messagepart.data.mapper.toMessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.attachments.data.mapper.toDomain
import com.cbgm.sparrow.feature.attachments.data.mapper.toDto
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.PreparedMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.attachments.runtime.MessageAttachmentCacheCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.cbgm.sparrow.protocol.attachment.MessageAttachment as ProtocolMessageAttachment

internal class MessageAttachmentOperationsRepositoryImpl(
    private val dataSource: MessageAttachmentDataSource,
    private val cacheCoordinator: MessageAttachmentCacheCoordinator
) : MessageAttachmentOperationsRepository {
    override suspend fun prepareAttachments(attachments: List<OutgoingMessageAttachment>): List<PreparedMessageAttachment> {
        MessageAttachmentPolicy.requireValid(attachments)
        return dataSource.prepareAttachments(
            attachments = attachments.map { it.toDto() },
            retentionMilliseconds = MessageAttachmentPolicy.DEFAULT_RETENTION_MILLISECONDS
        ).map { it.toDomain() }
    }

    override suspend fun persistOutgoing(
        messageId: String,
        prepared: List<PreparedMessageAttachment>,
        context: AttachmentMessageContext
    ) = dataSource.persistOutgoing(messageId, prepared.map { it.toDto() }, context.toDto())

    override suspend fun persistIncoming(
        messageId: String,
        attachments: List<ProtocolMessageAttachment>,
        context: AttachmentMessageContext
    ) = dataSource.persistIncoming(messageId, attachments, context.toDto())

    override suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean) =
        dataSource.updateConversationDisplayName(conversationId, displayName, isGroup)

    override suspend fun protocolAttachments(messageId: String): List<ProtocolMessageAttachment> =
        dataSource.protocolAttachments(messageId)

    override suspend fun loadDetachedBytes(attachment: ProtocolMessageAttachment): ByteArray =
        dataSource.loadDetachedBytes(attachment)

    override suspend fun deleteForMessages(messageIds: List<String>) = dataSource.deleteForMessages(messageIds)

    override suspend fun cleanupPrepared(prepared: List<PreparedMessageAttachment>) = dataSource.cleanupPrepared(prepared.map { it.toDto() })

    override fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>> =
        dataSource.observeByMessageIds(messageIds)
            .map { partsByMessageId ->
                partsByMessageId.mapValues { (_, parts) ->
                    parts.map { part -> part.toMessagePart() }
                }
            }

    override fun cacheIncoming(messageId: String) = cacheCoordinator.cache(messageId)
}
