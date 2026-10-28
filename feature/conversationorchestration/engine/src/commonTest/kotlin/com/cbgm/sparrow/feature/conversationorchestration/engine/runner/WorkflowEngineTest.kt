package com.cbgm.sparrow.feature.conversationorchestration.engine.runner

import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureException
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.ConversationFailureMapper
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.WorkflowEngineException
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorkflowEngineTest {
    @Test
    fun theEngineFindsTheWorkflowForACommandAndFollowsItsTransitions() =
        runTest {
            val engine = engine(CountingMachine())

            val result = engine.run(Count(2))

            assertEquals(ConversationResult.Done, result.getOrThrow())
        }

    @Test
    fun rejectedTransitionStopsWithItsFailure() =
        runTest {
            val engine = engine(CountingMachine())

            val error = engine.run(Count(-1)).exceptionOrNull()

            assertTrue(error is ConversationFailureException)
            assertEquals(NEGATIVE_COUNT, error.failure)
        }

    @Test
    fun thrownErrorKeepsItsOriginalCause() =
        runTest {
            val error = IllegalStateException("not allowed")
            val engine = engine(ThrowingMachine(error))

            assertSame(error, engine.run(Throw).exceptionOrNull())
        }

    @Test
    fun commandWithoutWorkflowFailsInsteadOfThrowing() =
        runTest {
            val engine = engine(CountingMachine())

            val error = engine.run(Throw).exceptionOrNull()

            assertTrue(error is ConversationFailureException)
            assertEquals(ConversationFailureReason.UNEXPECTED, error.failure.reason)
        }

    @Test
    fun endlessWorkflowFailsAsAnEngineErrorNotAsAForbiddenCommand() =
        runTest {
            val engine = engine(LoopingMachine())

            val error = engine.run(Loop).exceptionOrNull()

            assertTrue(error is WorkflowEngineException)
            assertEquals(ConversationFailureReason.UNEXPECTED, ConversationFailureMapper.toFailure(error).reason)
            assertEquals(
                ConversationFailureReason.NOT_ALLOWED,
                ConversationFailureMapper.toFailure(IllegalStateException("not allowed")).reason
            )
        }

    @Test
    fun twoWorkflowsForTheSameCommandAreRejectedByValidation() {
        val registry = WorkflowRegistry { listOf(CountingMachine(), CountingMachine()) }

        assertFailsWith<IllegalStateException> { registry.validate() }
    }

    @Test
    fun workflowsSharingALockKeyRunOneAfterAnother() =
        runTest {
            val release = CompletableDeferred<Unit>()
            val order = mutableListOf<String>()
            val engine = engine(SuspendingMachine(release, order))

            val first = async { engine.run(Suspend("first")) }
            yield()
            val second = async { engine.run(Suspend("second")) }
            yield()
            release.complete(Unit)
            first.await()
            second.await()

            assertEquals(listOf("first:start", "first:end", "second:start", "second:end"), order)
        }

    private fun engine(vararg workflows: WorkflowStateMachine<*, *, *>): WorkflowEngine =
        WorkflowEngine(WorkflowLocks(), WorkflowRegistry { workflows.toList() })

    private data class Count(
        val remaining: Int
    ) : WorkflowCommand

    private data object Throw : WorkflowCommand

    private data object Loop : WorkflowCommand

    private data class Suspend(
        val label: String
    ) : WorkflowCommand

    /** Counts the command down to zero, then finishes; a negative command is rejected. */
    private class CountingMachine : WorkflowStateMachine<Count, CountingMachine.Step, CountingMachine.Outcome> {
        override val commandType = Count::class

        override fun firstStep(command: Count): Step = Step.Count(command.remaining)

        override suspend fun perform(step: Step): Outcome =
            when (step) {
                is Step.Count -> if (step.remaining < 0) Outcome.Negative else Outcome.Counted(step.remaining - 1)
                Step.Finish -> Outcome.Finished
            }

        override fun transition(step: Step, outcome: Outcome): Transition<Step> =
            when (outcome) {
                is Outcome.Counted ->
                    if (outcome.remaining > 0) {
                        Transition.Next(Step.Count(outcome.remaining))
                    } else {
                        Transition.Next(Step.Finish)
                    }
                Outcome.Negative -> Transition.Reject(NEGATIVE_COUNT)
                Outcome.Finished -> Transition.Complete()
            }

        sealed interface Step : WorkflowStep {
            data class Count(
                val remaining: Int
            ) : Step

            data object Finish : Step
        }

        sealed interface Outcome {
            data class Counted(
                val remaining: Int
            ) : Outcome

            data object Negative : Outcome

            data object Finished : Outcome
        }
    }

    private class ThrowingMachine(
        private val error: Throwable
    ) : WorkflowStateMachine<Throw, ThrowingMachine.Step, Unit> {
        override val commandType = Throw::class

        override fun firstStep(command: Throw): Step = Step

        override suspend fun perform(step: Step): Unit = throw error

        override fun transition(step: Step, outcome: Unit): Transition<Step> = Transition.Complete()

        data object Step : WorkflowStep
    }

    /** Never completes, like a transition cycle. */
    private class LoopingMachine : WorkflowStateMachine<Loop, LoopingMachine.Step, Unit> {
        override val commandType = Loop::class

        override fun firstStep(command: Loop): Step = Step

        override suspend fun perform(step: Step) = Unit

        override fun transition(step: Step, outcome: Unit): Transition<Step> = Transition.Next(Step)

        data object Step : WorkflowStep
    }

    private class SuspendingMachine(
        private val release: CompletableDeferred<Unit>,
        private val order: MutableList<String>
    ) : WorkflowStateMachine<Suspend, SuspendingMachine.Step, Unit> {
        override val commandType = Suspend::class

        override fun firstStep(command: Suspend): Step = Step(command.label)

        override suspend fun perform(step: Step) {
            order += "${step.label}:start"
            release.await()
            order += "${step.label}:end"
        }

        override fun transition(step: Step, outcome: Unit): Transition<Step> = Transition.Complete()

        override fun lockKey(command: Suspend): String = "shared"

        data class Step(
            val label: String
        ) : WorkflowStep
    }

    private companion object {
        val NEGATIVE_COUNT =
            ConversationFailure(reason = ConversationFailureReason.INVALID_INPUT, message = "Negative count")
    }
}
