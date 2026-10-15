package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage

interface GroupMessageRepository {
    suspend fun findConversation(conversationId: String): Result<ConversationInfo?>

    suspend fun findParticipantIds(groupId: String): Result<List<String>>

    suspend fun hasMessageWithTransportMode(groupId: String, transportMode: String): Result<Boolean>

    suspend fun find(messageId: String): Result<StoredMessage?>

    suspend fun findText(messageId: String): Result<String?>

    suspend fun findQueued(groupId: String): Result<List<StoredMessage>>

    suspend fun findAwaitingOutbox(groupId: String): Result<List<StoredMessage>>

    suspend fun saveOutgoing(
        message: StoredMessage,
        text: String,
        recipientStates: List<GroupRecipientState>
    ): Result<Unit>

    suspend fun saveRecipientStates(states: List<GroupRecipientState>): Result<Unit>

    suspend fun findRecipientStates(messageId: String): Result<List<GroupRecipientState>>

    suspend fun findRecipientByPacketId(packetId: String): Result<GroupRecipientState?>

    suspend fun replaceText(messageId: String, text: String): Result<Unit>

    suspend fun delete(messages: List<StoredMessage>): Result<Unit>

    suspend fun findAwaitingReadReceipt(groupId: String): Result<List<UnreadIncomingMessage>>

    suspend fun markReadReceiptSent(messageId: String): Result<Boolean>

    /** Stores an incoming group message and moves its conversation to the top. */
    suspend fun saveIncoming(message: StoredMessage, text: String, receivedAtEpochMilliseconds: Long): Result<Unit>
}
