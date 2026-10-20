package com.cbgm.sparrow.feature.attachments.domain.repository

import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.base.messagepart.domain.model.PaymentTransfer
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
    ): Result<Unit>

    /** Synchronizes owner-provided names without exposing Chats or Contacts DAOs. */
    suspend fun updateConversationDisplayName(
        conversationId: String,
        displayName: String,
        isGroup: Boolean
    ): Result<Unit>

    suspend fun updateMessagePart(messageId: String, part: MessagePart): Result<Unit>

    suspend fun messageParts(messageId: String): Result<List<MessagePart>>

    /** Completes interrupted outgoing uploads before queued transport is resumed. */
    suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>>

    suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray>

    suspend fun deleteForMessages(messageIds: List<String>): Result<Unit>

    /** Chats owns paging and supplies the visible message IDs. */
    fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>>

    /** Observes a group's expenses in creation order; Chats decides which board they belong to. */
    fun observeExpenseState(conversationId: String): Flow<List<MessagePart>> =
        kotlinx.coroutines.flow.combine(observeExpenses(conversationId), observePaymentTransfers(conversationId)) { expenses, transfers ->
            expenses + transfers
        }

    fun observePaymentTransfers(conversationId: String): Flow<List<PaymentTransfer>> = kotlinx.coroutines.flow.flowOf(emptyList())

    fun observeExpenses(conversationId: String): Flow<List<Expense>>

    fun observeExpenseBoardClosures(conversationId: String): Flow<List<ExpenseBoardClosed>>

    suspend fun findExpenseMessageIds(conversationId: String, boardId: String): Result<List<String>>

    /** Non-blocking, deduplicated, best-effort caching after a message is received. */
    fun cacheIncoming(messageId: String): Result<Unit>
}
