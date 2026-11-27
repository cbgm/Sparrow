package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct.authorization

import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectRecipient
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.LoadDirectRecipientUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.StoreOutgoingDirectMessageUseCase
import kotlin.reflect.KClass

/**
 * CheckAuthorization -> (FindConversation -> LoadRecipient -> StoreWaitingMessage).
 *
 * Holds the contact's release lock and checks authorization again under it with the same rule the release uses:
 * either the contact became authorized meanwhile and nothing is stored (the caller sends the message normally), or
 * the message is stored as waiting before any later release can look for it. Only messages for contacts without
 * authorization take the lock, so authorized sends never wait for each other.
 */
class StoreWaitingDirectMessageWorkflow internal constructor(
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase,
    private val conversationRepository: DirectConversationRepository,
    private val loadDirectRecipient: LoadDirectRecipientUseCase,
    private val storeOutgoingDirectMessage: StoreOutgoingDirectMessageUseCase
) : WorkflowStateMachine<
        MessageCommand.StoreWaitingDirectMessage,
        StoreWaitingDirectMessageWorkflow.Step,
        StoreWaitingDirectMessageWorkflow.Outcome
    > {
    override val commandType: KClass<MessageCommand.StoreWaitingDirectMessage> =
        MessageCommand.StoreWaitingDirectMessage::class

    override fun lockKey(command: MessageCommand.StoreWaitingDirectMessage): String =
        directReleaseLockKey(command.contactId)

    override fun firstStep(command: MessageCommand.StoreWaitingDirectMessage): Step = Step.CheckAuthorization(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.CheckAuthorization ->
                findDirectAuthorizationBlock(step.command.contactId).getOrThrow()
                    ?.let { Outcome.StillUnauthorized }
                    ?: Outcome.AuthorizedMeanwhile
            is Step.FindConversation ->
                Outcome.ConversationFound(conversationRepository.requireTarget(step.command.conversationId).getOrThrow())
            is Step.LoadRecipient -> Outcome.RecipientLoaded(loadDirectRecipient(step.target).getOrThrow())
            is Step.StoreWaitingMessage -> {
                storeOutgoingDirectMessage(
                    recipient = step.recipient,
                    content = step.command.content,
                    deliveryStatus = MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION,
                    replyToMessageId = step.command.replyToMessageId
                ).getOrThrow()
                Outcome.MessageStored
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AuthorizedMeanwhile -> Transition.Complete(ConversationResult.Done)
            Outcome.StillUnauthorized -> Transition.Next(Step.FindConversation(step.command))
            is Outcome.ConversationFound -> Transition.Next(Step.LoadRecipient(outcome.target, step.command))
            is Outcome.RecipientLoaded -> Transition.Next(Step.StoreWaitingMessage(outcome.recipient, step.command))
            Outcome.MessageStored ->
                Transition.Complete(ConversationResult.DirectMessageQueued(step.command.authorizationFailure))
        }

    sealed interface Step : WorkflowStep {
        val command: MessageCommand.StoreWaitingDirectMessage

        data class CheckAuthorization(
            override val command: MessageCommand.StoreWaitingDirectMessage
        ) : Step

        data class FindConversation(
            override val command: MessageCommand.StoreWaitingDirectMessage
        ) : Step

        data class LoadRecipient(
            val target: DirectConversationTarget,
            override val command: MessageCommand.StoreWaitingDirectMessage
        ) : Step

        data class StoreWaitingMessage(
            val recipient: DirectRecipient,
            override val command: MessageCommand.StoreWaitingDirectMessage
        ) : Step
    }

    sealed interface Outcome {
        data object AuthorizedMeanwhile : Outcome

        data object StillUnauthorized : Outcome

        data class ConversationFound(
            val target: DirectConversationTarget
        ) : Outcome

        data class RecipientLoaded(
            val recipient: DirectRecipient
        ) : Outcome

        data object MessageStored : Outcome
    }
}
