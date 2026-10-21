package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper.toDomain
import com.cbgm.sparrow.feature.conversationorchestration.incoming.data.mapper.toParkedEntity
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.OperationConversation
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase.FindOperationTargetUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardGroupMessagesUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupPollUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.RequireMessageWithoutPaymentUseCase
import com.cbgm.sparrow.protocol.message.MessageOperation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Operations can overtake their message, for example when a reconnect delivers the mailbox out of order.
 * They are parked and applied once the message is stored.
 */
class ParkedMessageOperationsTest {
    private val store = MessageStore()
    private val parked = FakeParkedMessageOperationRepository()
    private val engine = workflowEngine()

    @Test
    fun editBeforeItsMessageIsAppliedWhenTheMessageArrives() = runBlocking {
        receive(edit("Fixed typo", sentAt = 2))
        assertEquals(1, parked.parked.size)

        storeMessage(text = "Fixd typo")
        assertEquals("Fixed typo", store.texts[MESSAGE_ID])
        assertTrue(parked.parked.isEmpty())
    }

    @Test
    fun deleteBeforeItsGroupMessageRemovesTheMessageWhenItArrives() = runBlocking {
        receive(operation(MessageOperation.Delete(MESSAGE_ID, 2), OperationConversation.GROUP, sentAt = 2))

        storeMessage(text = "Oops", transportMode = MessageTransportMode.GROUP_END_TO_END_ENCRYPTED)
        assertFalse(MESSAGE_ID in store.messages)
        assertTrue(parked.parked.isEmpty())
    }

    @Test
    fun reactionBeforeItsMessageIsSavedWhenTheMessageArrives() = runBlocking {
        receive(operation(MessageOperation.Reaction(MESSAGE_ID, EMOJI), OperationConversation.DIRECT, sentAt = 2))

        storeMessage(text = "Lunch?")
        assertEquals(setOf(Triple<String, String?, String>(MESSAGE_ID, SENDER, EMOJI)), store.reactions)
    }

    @Test
    fun parkedOperationsAreAppliedInTheOrderTheyWereSent() = runBlocking {
        receive(edit("Second edit", sentAt = 3))
        receive(edit("First edit", sentAt = 2))

        storeMessage(text = "Original")
        assertEquals("Second edit", store.texts[MESSAGE_ID])
    }

    @Test
    fun parkedOperationThatIsNotAllowedIsDroppedWithoutBlockingTheOthers() = runBlocking {
        receive(edit("Edited by someone else", sentAt = 2, sender = "mallory"))
        receive(edit("Edited by the sender", sentAt = 3))

        storeMessage(text = "Original")
        assertEquals("Edited by the sender", store.texts[MESSAGE_ID])
        assertTrue(parked.parked.isEmpty())
    }

    @Test
    fun operationOnAStoredMessageIsAppliedWithoutParking() = runBlocking {
        store.store(message(), "Original")

        receive(edit("Edited", sentAt = 2))
        assertEquals("Edited", store.texts[MESSAGE_ID])
        assertTrue(parked.parked.isEmpty())
    }

    @Test
    fun parkedOperationSurvivesTheRoundTripThroughItsTableRow() {
        val operation =
            operation(
                MessageOperation.PollVote(MESSAGE_ID, "poll", setOf("a", "b")),
                OperationConversation.GROUP,
                sentAt = 7
            )
        assertEquals(operation, operation.toParkedEntity(parkedAtEpochMilliseconds = 9).toDomain().operation)
    }

    private suspend fun receive(operation: IncomingCommand.ApplyRemoteMessageOperation) {
        engine.run(operation).getOrThrow()
    }

    /** What the receive workflows do once a message is stored. */
    private suspend fun storeMessage(text: String, transportMode: String = DIRECT_TRANSPORT) {
        store.store(message(transportMode), text)
        engine.run(IncomingCommand.ApplyParkedMessageOperations(MESSAGE_ID)).getOrThrow()
    }

    private fun workflowEngine(): WorkflowEngine {
        lateinit var engine: WorkflowEngine
        val attachments = FakeAttachmentRepository()
        val direct = FakeDirectMessageRepository(store)
        val group = FakeGroupMessageRepository(store)
        val registry =
            WorkflowRegistry {
                listOf(
                    ApplyRemoteMessageOperationWorkflow(
                        findOperationTarget = FindOperationTargetUseCase(direct, group),
                        directMessageRepository = direct,
                        groupMessageRepository = group,
                        attachmentRepository = attachments,
                        reactionRepository = FakeMessageReactionRepository(store),
                        discardDirectMessages = DiscardDirectMessagesUseCase(direct, attachments),
                        discardGroupMessages = DiscardGroupMessagesUseCase(group, attachments),
                        findGroupPoll = FindGroupPollUseCase(attachments),
                        membershipRepository = FakeGroupMembershipRepository(),
                        requireMessageWithoutPayment = RequireMessageWithoutPaymentUseCase(attachments),
                        parkedOperations = parked
                    ),
                    ApplyParkedMessageOperationsWorkflow(engine, parked)
                )
            }
        engine = WorkflowEngine(WorkflowLocks(), registry)
        return engine
    }

    private fun edit(text: String, sentAt: Long, sender: String = SENDER) =
        operation(MessageOperation.Edit(MESSAGE_ID, text, sentAt), OperationConversation.DIRECT, sentAt, sender)

    private fun operation(
        operation: MessageOperation,
        conversation: OperationConversation,
        sentAt: Long,
        sender: String = SENDER
    ) = IncomingCommand.ApplyRemoteMessageOperation(
        conversationId = CONVERSATION_ID,
        senderContactId = sender,
        operation = operation,
        conversation = conversation,
        sentAtEpochMilliseconds = sentAt
    )

    private fun message(transportMode: String = DIRECT_TRANSPORT) =
        StoredMessage(
            id = MESSAGE_ID,
            conversationId = CONVERSATION_ID,
            packetId = "packet",
            replyToMessageId = null,
            transportPayload = null,
            transportMode = transportMode,
            contentStatus = MessageContentStatus.READABLE,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
            senderContactId = SENDER,
            readReceiptSent = false,
            isMine = false,
            createdAtEpochMilliseconds = 1
        )

    private companion object {
        const val MESSAGE_ID = "message"
        const val CONVERSATION_ID = "conversation"
        const val SENDER = "alice"
        const val DIRECT_TRANSPORT = "E2EE"
        const val EMOJI = "+1"
    }
}
