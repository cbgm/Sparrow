package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import kotlin.reflect.KClass

/** FindWaitingMessages -> DiscardMessages. */
class DiscardWaitingDirectMessagesWorkflow internal constructor(
    private val messageRepository: DirectMessageRepository,
    private val discardDirectMessages: DiscardDirectMessagesUseCase
) : WorkflowStateMachine<
        MessageCommand.DiscardWaitingDirectMessages,
        DiscardWaitingDirectMessagesWorkflow.Step,
        DiscardWaitingDirectMessagesWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.DiscardWaitingDirectMessages> =
        MessageCommand.DiscardWaitingDirectMessages::class

    override fun lockKey(command: MessageCommand.DiscardWaitingDirectMessages): String =
        "direct-release:${command.contactId}"

    override fun firstStep(command: MessageCommand.DiscardWaitingDirectMessages): Step =
        Step.FindWaitingMessages(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindWaitingMessages ->
                Outcome.WaitingMessagesFound(
                    messageRepository.findWaitingForAuthorization(step.command.contactId).getOrThrow()
                )
            is Step.DiscardMessages -> {
                discardDirectMessages(step.messages).getOrThrow()
                Outcome.MessagesDiscarded
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.WaitingMessagesFound ->
                if (outcome.messages.isEmpty()) {
                    Transition.Complete()
                } else {
                    Transition.Next(Step.DiscardMessages(outcome.messages, step.command))
                }
            Outcome.MessagesDiscarded -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.DiscardWaitingDirectMessages

        data class FindWaitingMessages(
            override val command: MessageCommand.DiscardWaitingDirectMessages
        ) : Step

        data class DiscardMessages(
            val messages: List<StoredMessage>,
            override val command: MessageCommand.DiscardWaitingDirectMessages
        ) : Step
    }

    sealed interface Outcome {
        data class WaitingMessagesFound(
            val messages: List<StoredMessage>
        ) : Outcome

        data object MessagesDiscarded : Outcome
    }
}
