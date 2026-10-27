package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import kotlinx.coroutines.flow.Flow

interface DirectMessageRepository {
    suspend fun find(messageId: String): Result<StoredMessage?>

    suspend fun findText(messageId: String): Result<String?>

    suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit>

    suspend fun update(message: StoredMessage): Result<Unit>

    suspend fun replaceText(messageId: String, text: String): Result<Unit>

    suspend fun delete(messages: List<StoredMessage>): Result<Unit>

    suspend fun findWaitingForAuthorization(contactId: String): Result<List<StoredMessage>>

    fun observeWaitingForAuthorization(): Flow<List<StoredMessage>>

    /** Queued outgoing messages created before [createdBeforeEpochMilliseconds] whose packet is not in the outbox. */
    suspend fun findAwaitingOutbox(createdBeforeEpochMilliseconds: Long): Result<List<StoredMessage>>

    suspend fun findAwaitingReadReceipt(conversationId: String): Result<List<UnreadIncomingMessage>>

    suspend fun markReadReceiptSent(messageId: String): Result<Boolean>

    /** Stores an incoming message and moves its conversation to the top. */
    suspend fun saveIncoming(message: StoredMessage, text: String, receivedAtEpochMilliseconds: Long): Result<Unit>
}
