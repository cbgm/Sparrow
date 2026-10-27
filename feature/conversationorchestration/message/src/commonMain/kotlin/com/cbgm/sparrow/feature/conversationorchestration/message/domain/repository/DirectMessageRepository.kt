package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import kotlinx.coroutines.flow.Flow

interface DirectMessageRepository {
    suspend fun find(messageId: String): Result<StoredMessage?>

    suspend fun findText(messageId: String): Result<String?>

    suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit>

    suspend fun update(message: StoredMessage): Result<Unit>

    /**
     * Gives the message a new packet and [deliveryStatus] only while it is still in [expectedStatus], so a message
     * deleted or changed meanwhile is never written back. Returns whether the message was updated.
     */
    suspend fun updatePacketIfStatus(
        messageId: String,
        expectedStatus: MessageDeliveryStatus,
        packetId: String,
        transportMode: String,
        deliveryStatus: MessageDeliveryStatus
    ): Result<Boolean>

    /** A local edit: the text and its edit time are always replaced. */
    suspend fun replaceText(messageId: String, text: String, editedAtEpochMilliseconds: Long): Result<Unit>

    /**
     * A remote edit: replaces the text only when [editedAtEpochMilliseconds] is later than the stored edit, so
     * edits that arrive out of order keep the newest text (equal times keep the stored text). Returns whether
     * the text was replaced.
     */
    suspend fun replaceTextIfNewer(messageId: String, text: String, editedAtEpochMilliseconds: Long): Result<Boolean>

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
