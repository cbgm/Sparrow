package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import kotlinx.coroutines.flow.Flow

/** Attachment persistence and transfer API; callers never access attachment DAOs or datasources. */
interface MessageAttachmentOperationsRepository {
    suspend fun persistOutgoing(
        messageId: String,
        parts: List<MessagePart>,
        context: AttachmentMessageContext
    ): Result<List<MessagePart>>

    suspend fun persistIncoming(
        messageId: String,
        parts: List<MessagePart>,
        context: AttachmentMessageContext
    )

    /** Synchronizes owner-provided names without exposing Chats or Contacts DAOs. */
    suspend fun updateConversationDisplayName(conversationId: String, displayName: String, isGroup: Boolean)

    suspend fun messageParts(messageId: String): Result<List<MessagePart>>

    /** Completes interrupted outgoing uploads before queued transport is resumed. */
    suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>>

    suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray>

    suspend fun deleteForMessages(messageIds: List<String>)

    /** Chats owns paging and supplies the visible message IDs. */
    fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>>

    /** Non-blocking, deduplicated, best-effort caching after a message is received. */
    fun cacheIncoming(messageId: String)
}
