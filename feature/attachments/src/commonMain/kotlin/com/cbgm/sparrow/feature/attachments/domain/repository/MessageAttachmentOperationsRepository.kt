package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.PreparedMessageAttachment
import kotlinx.coroutines.flow.Flow
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment as ProtocolMessageAttachment

/** Attachment persistence and transfer API; callers never access attachment DAOs or datasources. */
interface MessageAttachmentOperationsRepository {
    suspend fun prepareAttachments(attachments: List<OutgoingMessageAttachment>): List<PreparedMessageAttachment>

    suspend fun persistOutgoing(messageId: String, prepared: List<PreparedMessageAttachment>, context: AttachmentMessageContext)

    suspend fun persistIncoming(messageId: String, attachments: List<ProtocolMessageAttachment>, context: AttachmentMessageContext)

    /** Synchronizes owner-provided names without exposing Chats or Contacts DAOs. */
    suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean)

    suspend fun protocolAttachments(messageId: String): List<ProtocolMessageAttachment>

    suspend fun loadDetachedBytes(attachment: ProtocolMessageAttachment): ByteArray

    suspend fun deleteForMessages(messageIds: List<String>)

    suspend fun cleanupPrepared(prepared: List<PreparedMessageAttachment>)

    /** Chats owns paging and supplies the visible message IDs. */
    fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessageAttachment>>>

    /** Non-blocking, deduplicated, best-effort caching after a message is received. */
    fun cacheIncoming(messageId: String)
}
