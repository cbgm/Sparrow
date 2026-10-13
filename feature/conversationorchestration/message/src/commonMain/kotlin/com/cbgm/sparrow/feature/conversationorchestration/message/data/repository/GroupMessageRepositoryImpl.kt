package com.cbgm.sparrow.feature.conversationorchestration.message.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.ConversationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.GroupMessageDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toGroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toMessageEntity
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toMessageRecipientStateEntity
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toStoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toUnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

internal class GroupMessageRepositoryImpl(
    private val dataSource: GroupMessageDataSource,
    private val conversationDataSource: ConversationDataSource
) : GroupMessageRepository {
    override suspend fun findConversation(conversationId: String): Result<ConversationInfo?> =
        safeSuspendCall { dataSource.findConversation(conversationId)?.toConversationInfo() }

    override suspend fun findParticipantIds(groupId: String): Result<List<String>> =
        safeSuspendCall { conversationDataSource.findParticipantIds(groupId).distinct() }

    override suspend fun hasMessageWithTransportMode(groupId: String, transportMode: String): Result<Boolean> =
        safeSuspendCall { dataSource.hasMessageWithTransportMode(groupId, transportMode) }

    override suspend fun find(messageId: String): Result<StoredMessage?> =
        safeSuspendCall { dataSource.findMessage(messageId)?.toStoredMessage() }

    override suspend fun findText(messageId: String): Result<String?> =
        safeSuspendCall { dataSource.findMessageText(messageId) }

    override suspend fun findQueued(groupId: String): Result<List<StoredMessage>> =
        safeSuspendCall { dataSource.findQueuedGroupMessages(groupId).map { it.toStoredMessage() } }

    override suspend fun findAwaitingOutbox(groupId: String): Result<List<StoredMessage>> =
        safeSuspendCall { dataSource.findGroupMessagesAwaitingOutbox(groupId).map { it.toStoredMessage() } }

    override suspend fun saveOutgoing(
        message: StoredMessage,
        text: String,
        recipientStates: List<GroupRecipientState>
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.saveOutgoingMessage(
                message = message.toMessageEntity(),
                text = text,
                recipientStates = recipientStates.map { it.toMessageRecipientStateEntity() },
                timestamp = message.createdAtEpochMilliseconds
            )
        }

    override suspend fun saveRecipientStates(states: List<GroupRecipientState>): Result<Unit> =
        safeSuspendCall { dataSource.saveRecipientStates(states.map { it.toMessageRecipientStateEntity() }) }

    override suspend fun findRecipientStates(messageId: String): Result<List<GroupRecipientState>> =
        safeSuspendCall { dataSource.findRecipientStates(messageId).map { it.toGroupRecipientState() } }

    override suspend fun findRecipientByPacketId(packetId: String): Result<GroupRecipientState?> =
        safeSuspendCall { dataSource.findRecipientByPacketId(packetId)?.toGroupRecipientState() }

    override suspend fun replaceText(messageId: String, text: String): Result<Unit> =
        safeSuspendCall { dataSource.replaceMessageText(messageId, text) }

    override suspend fun delete(messages: List<StoredMessage>): Result<Unit> =
        safeSuspendCall {
            if (messages.isNotEmpty()) dataSource.deleteMessages(messages.map(StoredMessage::toMessageEntity))
        }

    override suspend fun findAwaitingReadReceipt(groupId: String): Result<List<UnreadIncomingMessage>> =
        safeSuspendCall { dataSource.findAwaitingReadReceipt(groupId).map { it.toUnreadIncomingMessage() } }

    override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> =
        safeSuspendCall { dataSource.markReadReceiptSent(messageId) == 1 }

    override suspend fun saveIncoming(
        message: StoredMessage,
        text: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.saveIncomingMessage(message.toMessageEntity(), text)
            dataSource.updateConversationTimestamp(message.conversationId, receivedAtEpochMilliseconds)
        }
}
