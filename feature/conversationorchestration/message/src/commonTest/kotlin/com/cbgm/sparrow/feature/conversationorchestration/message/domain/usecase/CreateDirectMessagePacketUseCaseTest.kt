package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.base.messagepart.domain.model.Text
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.DirectMessagePacketFactory
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/** A packet must never point at an attachment that is not uploaded yet. */
class CreateDirectMessagePacketUseCaseTest {
    private val attachments = FakeAttachments()
    private val packetFactory = RecordingPacketFactory()
    private val useCase = CreateDirectMessagePacketUseCase(FakeMessages(), attachments, packetFactory)

    @Test
    fun releasedMessageUploadsItsAttachmentsBeforeThePacketIsBuilt() = runBlocking {
        useCase.forRelease(message())
        assertEquals(listOf(MESSAGE_ID), attachments.prepared)
        assertEquals(listOf(UPLOADED_PART), packetFactory.parts)
    }

    @Test
    fun retriedMessageUploadsItsAttachmentsBeforeThePacketIsBuilt() = runBlocking {
        useCase.forRetry(message())
        assertEquals(listOf(MESSAGE_ID), attachments.prepared)
        assertEquals(listOf(UPLOADED_PART), packetFactory.parts)
    }

    private fun message() =
        StoredMessage(
            id = MESSAGE_ID,
            conversationId = "conversation",
            packetId = null,
            replyToMessageId = null,
            transportPayload = null,
            transportMode = "SEALED_BOX",
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
            senderContactId = null,
            readReceiptSent = false,
            isMine = true,
            createdAtEpochMilliseconds = 1
        )

    /** Stops after recording the parts; the packet itself does not matter here. */
    private class RecordingPacketFactory : DirectMessagePacketFactory {
        var parts: List<MessagePart>? = null

        override suspend fun create(
            messageId: String,
            text: String,
            parts: List<MessagePart>,
            replyToMessageId: String?
        ): Result<ChatMessagePacket> {
            this.parts = parts
            return Result.failure(IllegalStateException("Packet not needed"))
        }
    }

    private class FakeAttachments : MessageAttachmentOperationsRepository {
        val prepared = mutableListOf<String>()

        override suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>> {
            prepared += messageId
            return Result.success(listOf(UPLOADED_PART))
        }

        override suspend fun messageParts(messageId: String): Result<List<MessagePart>> = error(NOT_USED)

        override suspend fun persistOutgoing(
            messageId: String,
            parts: List<MessagePart>,
            context: AttachmentMessageContext
        ): Result<List<MessagePart>> = error(NOT_USED)

        override suspend fun persistIncoming(
            messageId: String,
            parts: List<MessagePart>,
            context: AttachmentMessageContext
        ): Result<Unit> = error(NOT_USED)

        override suspend fun updateConversationDisplayName(
            conversationId: String,
            displayName: String,
            isGroup: Boolean
        ): Result<Unit> = error(NOT_USED)

        override suspend fun updateMessagePart(messageId: String, part: MessagePart): Result<Unit> = error(NOT_USED)

        override suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray> = error(NOT_USED)

        override suspend fun deleteForMessages(messageIds: List<String>): Result<Unit> = error(NOT_USED)

        override fun observeByMessageIds(messageIds: List<String>): Flow<Map<String, List<MessagePart>>> =
            error(NOT_USED)

        override fun observeExpenses(conversationId: String): Flow<List<Expense>> = error(NOT_USED)

        override fun observeExpenseBoardClosures(conversationId: String): Flow<List<ExpenseBoardClosed>> =
            error(NOT_USED)

        override suspend fun findExpenseMessageIds(conversationId: String, boardId: String): Result<List<String>> =
            error(NOT_USED)

        override fun cacheIncoming(messageId: String): Result<Unit> = error(NOT_USED)
    }

    private class FakeMessages : DirectMessageRepository {
        override suspend fun findText(messageId: String): Result<String?> = Result.success("")

        override suspend fun find(messageId: String): Result<StoredMessage?> = error(NOT_USED)

        override suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit> = error(NOT_USED)

        override suspend fun update(message: StoredMessage): Result<Unit> = error(NOT_USED)

        override suspend fun updatePacketIfStatus(
            messageId: String,
            expectedStatus: MessageDeliveryStatus,
            packetId: String,
            transportMode: String,
            deliveryStatus: MessageDeliveryStatus
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun replaceText(
            messageId: String,
            text: String,
            editedAtEpochMilliseconds: Long
        ): Result<Unit> = error(NOT_USED)

        override suspend fun replaceTextIfNewer(
            messageId: String,
            text: String,
            editedAtEpochMilliseconds: Long
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun delete(messages: List<StoredMessage>): Result<Unit> = error(NOT_USED)

        override suspend fun findWaitingForAuthorization(contactId: String): Result<List<StoredMessage>> =
            error(NOT_USED)

        override fun observeWaitingForAuthorization(): Flow<List<StoredMessage>> = error(NOT_USED)

        override suspend fun findAwaitingOutbox(createdBeforeEpochMilliseconds: Long): Result<List<StoredMessage>> =
            error(NOT_USED)

        override suspend fun findAwaitingReadReceipt(conversationId: String): Result<List<UnreadIncomingMessage>> =
            error(NOT_USED)

        override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> = error(NOT_USED)

        override suspend fun saveIncoming(
            message: StoredMessage,
            text: String,
            receivedAtEpochMilliseconds: Long
        ): Result<Unit> = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used"
        const val MESSAGE_ID = "message"
        val UPLOADED_PART: MessagePart = Text(id = "text", text = "uploaded")
    }
}
