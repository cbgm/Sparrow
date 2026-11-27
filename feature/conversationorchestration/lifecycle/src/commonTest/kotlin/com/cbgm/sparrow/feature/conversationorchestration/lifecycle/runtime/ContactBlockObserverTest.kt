package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/** The observer runs for the app's lifetime, so neither a failed block nor a failed observation may stop it. */
class ContactBlockObserverTest {
    @Test
    fun failedBlockIsRetriedWithoutAnotherBlocklistChange() =
        runTest {
            val handler = BlockHandler(failures = 2)
            val observer = ContactBlockObserver(Blocklist(flow { emit(setOf(PEER_ID)) }), engine(handler))

            observer.run()

            assertEquals(listOf(PEER_ID, PEER_ID, PEER_ID), handler.attempts)
        }

    @Test
    fun failedObservationIsSubscribedAgain() =
        runTest {
            var subscriptions = 0
            val blocked =
                flow {
                    subscriptions += 1
                    check(subscriptions > 1) { "database is closed" }
                    emit(setOf(PEER_ID))
                }
            val handler = BlockHandler(failures = 0)

            ContactBlockObserver(Blocklist(blocked), engine(handler)).run()

            assertEquals(2, subscriptions)
            assertEquals(listOf(PEER_ID), handler.attempts)
        }

    @Test
    fun handledContactIsNotHandledAgain() =
        runTest {
            val handler = BlockHandler(failures = 0)
            val blocked =
                flow {
                    emit(setOf(PEER_ID))
                    emit(setOf(PEER_ID, OTHER_PEER_ID))
                }

            ContactBlockObserver(Blocklist(blocked), engine(handler)).run()

            assertEquals(listOf(PEER_ID, OTHER_PEER_ID), handler.attempts)
        }

    private fun engine(handler: BlockHandler) = WorkflowEngine(WorkflowLocks(), WorkflowRegistry { listOf(handler) })

    /** Fails the first [failures] attempts. */
    private class BlockHandler(
        private var failures: Int
    ) : WorkflowStateMachine<LifecycleCommand.HandleContactBlocked, BlockHandler.Handle, Unit> {
        val attempts = mutableListOf<String>()

        override val commandType: KClass<LifecycleCommand.HandleContactBlocked> =
            LifecycleCommand.HandleContactBlocked::class

        override fun firstStep(command: LifecycleCommand.HandleContactBlocked) = Handle(command.contactId)

        override suspend fun perform(step: Handle) {
            attempts += step.contactId
            if (failures > 0) {
                failures -= 1
                error("node unreachable")
            }
        }

        override fun transition(step: Handle, outcome: Unit): Transition<Handle> = Transition.Complete()

        data class Handle(
            val contactId: String
        ) : WorkflowStep
    }

    private class Blocklist(
        private val blocked: Flow<Set<String>>
    ) : ContactBlocklistRepository {
        override fun observeBlockedContactIds(): Flow<Set<String>> = blocked

        override fun observeBlockUnknownContactInvites(): Flow<Boolean> = error(NOT_USED)

        override suspend fun getBlockUnknownContactInvites(): Result<Boolean> = error(NOT_USED)

        override suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit> = error(NOT_USED)

        override suspend fun getBlockedContactIds(): Result<Set<String>> = error(NOT_USED)

        override suspend fun isBlocked(contactId: String): Result<Boolean> = error(NOT_USED)

        override suspend fun block(contactId: String): Result<Unit> = error(NOT_USED)

        override suspend fun unblock(contactId: String): Result<Unit> = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used"
        const val PEER_ID = "peer-1"
        const val OTHER_PEER_ID = "peer-2"
    }
}
