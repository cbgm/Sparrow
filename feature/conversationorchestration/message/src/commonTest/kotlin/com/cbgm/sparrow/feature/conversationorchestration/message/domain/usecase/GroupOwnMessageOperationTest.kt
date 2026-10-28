package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.core.base.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoardClosed
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.base.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.base.messagepart.domain.model.PollOption
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentMessageContext
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.ConversationInfo
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.GroupRecipientState
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Edits and deletes of an own group message reach only those who can have it. A message still queued because
 * every other member is pending stays local, and the later flush sends its current content or nothing.
 */
class GroupOwnMessageOperationTest {
    private val messages = FakeGroupMessages()
    private val attachments = FakePollAttachments()
    private val recipients = FindGroupRecipientsUseCase(messages)

    @Test
    fun queuedMessageWithoutActiveMembersChangesLocallyOnly() = runBlocking<Unit> {
        messages.queued += queuedMessage()
        assertTrue(recipients.forOwnMessage(GROUP_ID, MESSAGE_ID).getOrThrow().isEmpty())
    }

    @Test
    fun queuedMessageNobodyReceivedYetChangesLocallyOnlyEvenWhenMembersBecameActive() = runBlocking<Unit> {
        messages.queued += queuedMessage()
        messages.participants += "alice"
        assertTrue(recipients.forOwnMessage(GROUP_ID, MESSAGE_ID).getOrThrow().isEmpty())
    }

    @Test
    fun sentMessageChangeGoesToTheActiveMembers() = runBlocking<Unit> {
        messages.participants += listOf("alice", "bob")
        messages.states += recipientState("alice")
        assertEquals(listOf("alice", "bob"), recipients.forOwnMessage(GROUP_ID, MESSAGE_ID).getOrThrow())
    }

    @Test
    fun deletedQueuedMessageIsNotFlushed() = runBlocking<Unit> {
        messages.queued += queuedMessage()
        DiscardGroupMessagesUseCase(messages, attachments)(listOf(queuedMessage())).getOrThrow()
        assertTrue(FindGroupMessagesToFlushUseCase(messages)(GROUP_ID).getOrThrow().isEmpty())
    }

    @Test
    fun closedPollIsRefusedBeforeTheCloseIsSent() = runBlocking<Unit> {
        attachments.parts = listOf(poll(closedAt = 5L))
        val error = FindGroupPollUseCase(attachments).closing(MESSAGE_ID, POLL_ID, 9L).exceptionOrNull()
        assertIs<IllegalArgumentException>(error)
        assertEquals("Poll is already closed", error.message)
    }

    @Test
    fun openPollIsClosedInMemoryOnly() = runBlocking<Unit> {
        attachments.parts = listOf(poll(closedAt = null))
        val closed = FindGroupPollUseCase(attachments).closing(MESSAGE_ID, POLL_ID, 9L).getOrThrow()
        assertEquals(9L, closed.closedAtEpochMilliseconds)
        assertTrue(attachments.updated.isEmpty())
    }

    private fun poll(closedAt: Long?) =
        Poll(
            id = POLL_ID,
            question = "Lunch?",
            options = listOf(PollOption("pizza", "Pizza"), PollOption("soup", "Soup")),
            closedAtEpochMilliseconds = closedAt
        )

    private fun queuedMessage() =
        StoredMessage(
            id = MESSAGE_ID,
            conversationId = GROUP_ID,
            packetId = null,
            replyToMessageId = null,
            transportPayload = null,
            transportMode = MessageTransportMode.GROUP_END_TO_END_ENCRYPTED,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.QUEUED,
            senderContactId = null,
            readReceiptSent = false,
            isMine = true,
            createdAtEpochMilliseconds = 1L
        )

    private fun recipientState(contactId: String) =
        GroupRecipientState(
            messageId = MESSAGE_ID,
            contactId = contactId,
            packetId = "packet-$contactId",
            deliveryStatus = MessageDeliveryStatus.SENT,
            lastError = null,
            updatedAtEpochMilliseconds = 2L
        )

    private class FakeGroupMessages : GroupMessageRepository {
        val participants = mutableListOf<String>()
        val queued = mutableListOf<StoredMessage>()
        val states = mutableListOf<GroupRecipientState>()

        override suspend fun findParticipantIds(groupId: String): Result<List<String>> = Result.success(participants)

        override suspend fun findRecipientStates(messageId: String): Result<List<GroupRecipientState>> =
            Result.success(states.filter { it.messageId == messageId })

        override suspend fun findQueued(groupId: String): Result<List<StoredMessage>> = Result.success(queued.toList())

        override suspend fun findAwaitingOutbox(groupId: String): Result<List<StoredMessage>> =
            Result.success(emptyList())

        override suspend fun delete(messages: List<StoredMessage>): Result<Unit> {
            queued.removeAll { stored -> messages.any { it.id == stored.id } }
            return Result.success(Unit)
        }

        override suspend fun findConversation(conversationId: String): Result<ConversationInfo?> = error(NOT_USED)

        override suspend fun hasMessageWithTransportMode(groupId: String, transportMode: String): Result<Boolean> =
            error(NOT_USED)

        override suspend fun find(messageId: String): Result<StoredMessage?> = error(NOT_USED)

        override suspend fun findText(messageId: String): Result<String?> = error(NOT_USED)

        override suspend fun saveOutgoing(
            message: StoredMessage,
            text: String,
            recipientStates: List<GroupRecipientState>
        ): Result<Unit> = error(NOT_USED)

        override suspend fun saveRecipientStates(states: List<GroupRecipientState>): Result<Unit> = error(NOT_USED)

        override suspend fun findRecipientByPacketId(packetId: String): Result<GroupRecipientState?> =
            error(NOT_USED)

        override suspend fun replaceText(messageId: String, text: String, editedAtEpochMilliseconds: Long): Result<Unit> =
            error(NOT_USED)

        override suspend fun replaceTextIfNewer(
            messageId: String,
            text: String,
            editedAtEpochMilliseconds: Long
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun findAwaitingReadReceipt(groupId: String): Result<List<UnreadIncomingMessage>> =
            error(NOT_USED)

        override suspend fun markReadReceiptSent(messageId: String): Result<Boolean> = error(NOT_USED)

        override suspend fun saveIncoming(
            message: StoredMessage,
            text: String,
            receivedAtEpochMilliseconds: Long
        ): Result<Unit> = error(NOT_USED)
    }

    private class FakePollAttachments : MessageAttachmentOperationsRepository {
        var parts: List<MessagePart> = emptyList()
        val updated = mutableListOf<MessagePart>()

        override suspend fun messageParts(messageId: String): Result<List<MessagePart>> = Result.success(parts)

        override suspend fun deleteForMessages(messageIds: List<String>): Result<Unit> = Result.success(Unit)

        override suspend fun updateMessagePart(messageId: String, part: MessagePart): Result<Unit> {
            updated += part
            return Result.success(Unit)
        }

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

        override suspend fun prepareOutgoing(messageId: String): Result<List<MessagePart>> = error(NOT_USED)

        override suspend fun loadDetachedBytes(part: MessagePart): Result<ByteArray> = error(NOT_USED)

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
        const val NOT_USED = "Not used by own group message operations"
        const val GROUP_ID = "group-1"
        const val MESSAGE_ID = "message-1"
        const val POLL_ID = "poll-1"
    }
}
