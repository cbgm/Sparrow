package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import kotlin.reflect.KClass

/** EnsureConversation -> ReleaseWaitingMessages. */
class ActivateAuthorizedConversationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: DirectConversationRepository
) : WorkflowStateMachine<
        MessageCommand.ActivateAuthorizedConversation,
        ActivateAuthorizedConversationWorkflow.Step,
        ActivateAuthorizedConversationWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.ActivateAuthorizedConversation> =
        MessageCommand.ActivateAuthorizedConversation::class

    override fun firstStep(command: MessageCommand.ActivateAuthorizedConversation): Step =
        Step.EnsureConversation(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.EnsureConversation -> {
                conversationRepository.getOrCreate(step.command.contactId).getOrThrow()
                Outcome.ConversationReady
            }
            is Step.ReleaseWaitingMessages -> {
                engine.run(MessageCommand.ReleaseWaitingDirectMessages(step.command.contactId)).getOrThrow()
                Outcome.WaitingMessagesReleased
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.ConversationReady -> Transition.Next(Step.ReleaseWaitingMessages(step.command))
            Outcome.WaitingMessagesReleased -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.ActivateAuthorizedConversation

        data class EnsureConversation(
            override val command: MessageCommand.ActivateAuthorizedConversation
        ) : Step

        data class ReleaseWaitingMessages(
            override val command: MessageCommand.ActivateAuthorizedConversation
        ) : Step
    }

    sealed interface Outcome {
        data object ConversationReady : Outcome

        data object WaitingMessagesReleased : Outcome
    }
}
