package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.messagepart.data.model.TextDto
import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The list of waiting messages a release walks may be stale; a message is read again before it is sent. */
class ReleaseWaitingDirectMessageUseCaseTest {
    private val messages = FakeMessages()
    private val packets = RecordingPacketFactory()
    private val useCase =
        ReleaseWaitingDirectMessageUseCase(
            messages,
            CreateDirectMessagePacketUseCase(messages, NoAttachments, packets)
        )

    @Test
    fun waitingMessageIsSentWithItsCurrentText() =
        runBlocking {
            messages.stored = waitingMessage()
            messages.text = "edited"

            val packet = useCase(MESSAGE_ID).getOrThrow()

            assertEquals(listOf("edited"), packets.texts)
            assertEquals(MessageDeliveryStatus.QUEUED, messages.stored?.deliveryStatus)
            assertEquals(packet?.packetId, messages.stored?.packetId)
        }

    @Test
    fun deletedMessageIsSkippedAndNeverWrittenBack() =
        runBlocking {
            messages.stored = null

            assertNull(useCase(MESSAGE_ID).getOrThrow())
            assertTrue(packets.texts.isEmpty())
            assertNull(messages.stored)
        }

    @Test
    fun messageNoLongerWaitingIsSkipped() =
        runBlocking {
            messages.stored = waitingMessage().copy(deliveryStatus = MessageDeliveryStatus.QUEUED)

            assertNull(useCase(MESSAGE_ID).getOrThrow())
            assertTrue(packets.texts.isEmpty())
        }

    @Test
    fun messageDeletedWhilePreparingIsSkipped() =
        runBlocking {
            messages.stored = waitingMessage()
            packets.beforeReturn = { messages.stored = null }

            assertNull(useCase(MESSAGE_ID).getOrThrow())
            assertNull(messages.stored)
        }

    private fun waitingMessage() =
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

    private class RecordingPacketFactory : DirectMessagePacketFactory {
        val texts = mutableListOf<String>()
        var beforeReturn: () -> Unit = {}

        override suspend fun create(
            messageId: String,
            text: String,
            parts: List<MessagePart>,
            replyToMessageId: String?
        ): Result<ChatMessagePacket> {
            texts += text
            beforeReturn()
            return Result.success(
                ChatMessagePacket(
                    packetId = "packet-${texts.size}",
                    messageId = messageId,
                    sentAtEpochMilliseconds = 1,
                    parts = listOf(TextDto(id = messageId, text = text))
                )
            )
        }
    }

    /** One stored message; the conditional update behaves like the SQL WHERE clause. */
    private class FakeMessages : DirectMessageRepository {
        var stored: StoredMessage? = null
        var text: String = "hello"

        override suspend fun find(messageId: String): Result<StoredMessage?> = Result.success(stored)

        override suspend fun findText(messageId: String): Result<String?> = Result.success(text)

        override suspend fun updatePacketIfStatus(
            messageId: String,
            expectedStatus: MessageDeliveryStatus,
            packetId: String,
            transportMode: String,
            deliveryStatus: MessageDeliveryStatus
        ): Result<Boolean> {
            val current = stored?.takeIf { it.id == messageId && it.deliveryStatus == expectedStatus }
                ?: return Result.success(false)
            stored = current.copy(packetId = packetId, transportMode = transportMode, deliveryStatus = deliveryStatus)
            return Result.success(true)
        }

        override suspend fun saveWithText(message: StoredMessage, text: String): Result<Unit> = error(NOT_USED)

        override suspend fun update(message: StoredMessage): Result<Unit> = error(NOT_USED)

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

    private object NoAttachments : MessageAttachmentOperationsRepository {
        override suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>> =
            Result.success(emptyList())

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

    private companion object {
        const val NOT_USED = "Not used"
        const val MESSAGE_ID = "message"
    }
}
