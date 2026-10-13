package com.cbgm.sparrow.feature.conversationorchestration.engine.orchestrator

import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationState
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultConversationOrchestratorTest {
    private val orchestrator =
        DefaultConversationOrchestrator(
            WorkflowEngine(WorkflowLocks(), WorkflowRegistry { listOf(OpenConversationWorkflow()) })
        )

    @Test
    fun commandIsExecutedByTheWorkflowRegisteredForItsTypeAndEveryStepIsPublished() =
        runTest {
            val states = orchestrator.execute(DirectConversationCommand.OpenConversation("contact")).toList()

            assertEquals(
                listOf(
                    ConversationState.Running("Step"),
                    ConversationState.Completed(ConversationResult.ConversationOpened("conversation-contact"))
                ),
                states
            )
        }

    @Test
    fun commandWithoutWorkflowFailsInsteadOfThrowing() =
        runTest {
            val finalState = orchestrator.execute(DirectConversationCommand.DeleteConversation("conversation")).last()

            assertTrue(finalState is ConversationState.Failed)
            assertEquals(ConversationFailureReason.UNEXPECTED, finalState.failure.reason)
        }

    private class OpenConversationWorkflow : ConversationWorkflow<DirectConversationCommand.OpenConversation, OpenConversationWorkflow.Step, String> {
        override val commandType = DirectConversationCommand.OpenConversation::class

        override fun firstStep(command: DirectConversationCommand.OpenConversation): Step = Step(command)

        override suspend fun perform(step: Step): String = "conversation-${step.command.contactId}"

        override fun transition(step: Step, outcome: String): Transition<Step> =
            Transition.Complete(ConversationResult.ConversationOpened(outcome))

        data class Step(
            val command: DirectConversationCommand.OpenConversation
        ) : WorkflowStep
    }
}
