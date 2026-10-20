package com.cbgm.sparrow.feature.conversationorchestration.message.data.repository

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.DirectMessageDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toMessageEntity
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toStoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.data.mapper.toUnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DirectMessageRepositoryImpl(
    private val dataSource: DirectMessageDataSource
) : DirectMessageRepository {
    override suspend fun find(messageId: String): Result<StoredMessage?> =
        safeSuspendCall { dataSource.findMessage(messageId)?.toStoredMessage() }

    override suspend fun findText(messageId: String): Result<String?> =
        safeSuspendCall { dataSource.findMessageText(messageId) }

    override suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit> =
        safeSuspendCall { dataSource.upsertMessageWithText(message.toMessageEntity(), text) }

    override suspend fun update(message: StoredMessage): Result<Unit> =
        safeSuspendCall { dataSource.upsertMessage(message.toMessageEntity()) }

    override suspend fun replaceText(messageId: String, text: String): Result<Unit> =
        safeSuspendCall { dataSource.replaceMessageText(messageId, text) }

    override suspend fun delete(messages: List<StoredMessage>): Result<Unit> =
        safeSuspendCall {
            if (messages.isNotEmpty()) dataSource.deleteMessages(messages.map(StoredMessage::toMessageEntity))
        }

    override suspend fun findWaitingForAuthorization(contactId: String): Result<List<StoredMessage>> =
        safeSuspendCall { dataSource.findMessagesWaitingForAuthorization(contactId).map { it.toStoredMessage() } }

    override fun observeWaitingForAuthorization(): Flow<List<StoredMessage>> =
        dataSource.observeMessagesWaitingForAuthorization().map { messages -> messages.map { it.toStoredMessage() } }

    override suspend fun findAwaitingReadReceipt(conversationId: String): Result<List<UnreadIncomingMessage>> =
        safeSuspendCall {
            dataSource.findMessagesAwaitingReadReceipt(conversationId).map { it.toUnreadIncomingMessage() }
        }

    override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> =
        safeSuspendCall { dataSource.markReadReceiptSent(messageId) == 1 }

    override suspend fun saveIncoming(
        message: StoredMessage,
        text: String,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            val conversation = dataSource.findConversation(message.conversationId)
                ?: error("Direct conversation was not found")
            dataSource.upsertIncomingChatMessage(
                conversation = conversation,
                message = message.toMessageEntity(),
                text = text,
                timestamp = receivedAtEpochMilliseconds
            )
        }
}
