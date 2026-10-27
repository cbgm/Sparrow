package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.base.messagepart.domain.model.Text
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.OutgoingMessageContent
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.StoreWaitingDirectMessageWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization.directReleaseLockKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A message to an authorized contact is stored and shown without any lock, so it never waits for an earlier send
 * to the same contact (which may still upload attachments) or for a release. Only storing a waiting message holds
 * the release lock, and it checks the authorization again under it.
 */
class DirectSendLockTest {
    private val authorization = DirectAuthorizationFakes()
    private val conversations = FakeDirectConversations()
    private val delivery = RecordingDelivery()
    private val locks = WorkflowLocks()
    private val send =
        SendDirectMessageWorkflow(
            engine = WorkflowEngineHolder.engine,
            requireDirectChatAuthorization = authorization.requireDirectChatAuthorization,
            inspectDirectInvitation = authorization.inspectDirectInvitation,
            reauthorization = NoReauthorization,
            conversationRepository = conversations
        )
    private val storeWaiting =
        StoreWaitingDirectMessageWorkflow(
            findDirectAuthorizationBlock = authorization.findDirectAuthorizationBlock,
            conversationRepository = conversations,
            loadDirectRecipient = unusedLoadDirectRecipient,
            storeOutgoingDirectMessage = unusedStoreOutgoingDirectMessage
        )

    @Test
    fun onlyStoringAWaitingMessageTakesTheReleaseLock() {
        assertNull(send.lockKey(sendCommand("hello")))
        assertEquals(directReleaseLockKey(CONTACT_ID), storeWaiting.lockKey(storeCommand()))
    }

    @Test
    fun authorizedSendsToOneContactDoNotWaitForEachOtherOrForARelease() =
        runBlocking {
            val engine = engine()
            delivery.blockFirst = CompletableDeferred()
            val releaseHeld = CompletableDeferred<Unit>()
            val releaseDone = CompletableDeferred<Unit>()
            val release =
                launch {
                    locks.withLock(directReleaseLockKey(CONTACT_ID)) {
                        releaseHeld.complete(Unit)
                        releaseDone.await()
                    }
                }
            releaseHeld.await()

            val first = async { engine.run(sendCommand("first")) }
            withTimeout(5_000) { delivery.started.first { it == 1 } }
            val second = withTimeout(5_000) { engine.run(sendCommand("second")).getOrThrow() }

            assertEquals(ConversationResult.DirectMessageSent, second)
            assertEquals(listOf("first", "second"), delivery.texts)
            delivery.blockFirst?.complete(Unit)
            assertEquals(ConversationResult.DirectMessageSent, first.await().getOrThrow())
            releaseDone.complete(Unit)
            release.join()
        }

    @Test
    fun waitingMessageIsDeliveredWhenTheContactWasAuthorizedWhileItWaitedForTheLock() =
        runBlocking {
            val engine = engine()
            authorization.startIdentityChange(CONTACT_ID)
            val releaseHeld = CompletableDeferred<Unit>()
            val releaseDone = CompletableDeferred<Unit>()
            val release =
                launch {
                    locks.withLock(directReleaseLockKey(CONTACT_ID)) {
                        releaseHeld.complete(Unit)
                        releaseDone.await()
                    }
                }
            releaseHeld.await()

            val sent = async { engine.run(sendCommand("hello")) }
            withTimeout(5_000) { conversations.resolved.first { it == 1 } }
            assertTrue(delivery.texts.isEmpty())
            authorization.approveIdentityChange()
            releaseDone.complete(Unit)

            assertEquals(ConversationResult.DirectMessageSent, sent.await().getOrThrow())
            assertEquals(listOf("hello"), delivery.texts)
            release.join()
        }

    @Test
    fun contactStillWithoutAuthorizationUnderTheLockGetsAWaitingMessage() =
        runBlocking {
            authorization.startIdentityChange(CONTACT_ID)
            val step = StoreWaitingDirectMessageWorkflow.Step.CheckAuthorization(storeCommand())

            val outcome = storeWaiting.perform(step)

            assertEquals(StoreWaitingDirectMessageWorkflow.Outcome.StillUnauthorized, outcome)
            assertEquals(
                Transition.Next(StoreWaitingDirectMessageWorkflow.Step.FindConversation(storeCommand())),
                storeWaiting.transition(step, outcome)
            )
        }

    private fun engine(): WorkflowEngine {
        lateinit var engine: WorkflowEngine
        engine =
            WorkflowEngine(
                locks,
                WorkflowRegistry {
                    listOf(
                        SendDirectMessageWorkflow(
                            engine = engine,
                            requireDirectChatAuthorization = authorization.requireDirectChatAuthorization,
                            inspectDirectInvitation = authorization.inspectDirectInvitation,
                            reauthorization = NoReauthorization,
                            conversationRepository = conversations
                        ),
                        storeWaiting,
                        delivery
                    )
                }
            )
        return engine
    }

    private fun sendCommand(text: String) =
        DirectConversationCommand.SendMessage(contactId = CONTACT_ID, parts = listOf(Text(id = "part-$text", text = text)))

    private fun storeCommand() =
        MessageCommand.StoreWaitingDirectMessage(
            contactId = CONTACT_ID,
            conversationId = CONVERSATION_ID,
            content = OutgoingMessageContent(text = "hello", attachments = emptyList()),
            replyToMessageId = null,
            authorizationFailure = null
        )

    /** Records every delivered text; the first delivery waits for [blockFirst] like a slow attachment upload. */
    private class RecordingDelivery : WorkflowStateMachine<MessageCommand.DeliverDirectMessage, RecordingDelivery.Deliver, Unit> {
        val texts = mutableListOf<String>()
        val started = kotlinx.coroutines.flow.MutableStateFlow(0)
        var blockFirst: CompletableDeferred<Unit>? = null

        override val commandType: KClass<MessageCommand.DeliverDirectMessage> =
            MessageCommand.DeliverDirectMessage::class

        override fun firstStep(command: MessageCommand.DeliverDirectMessage): Deliver = Deliver(command)

        override suspend fun perform(step: Deliver) {
            texts += step.command.content.text
            started.value++
            if (started.value == 1) blockFirst?.await()
        }

        override fun transition(step: Deliver, outcome: Unit): Transition<Deliver> = Transition.Complete()

        class Deliver(
            val command: MessageCommand.DeliverDirectMessage
        ) : WorkflowStep
    }

    /** The lock key is read without running the workflow, so no engine is needed for [send]. */
    private object WorkflowEngineHolder {
        val engine = WorkflowEngine(WorkflowLocks(), WorkflowRegistry { emptyList() })
    }
}
