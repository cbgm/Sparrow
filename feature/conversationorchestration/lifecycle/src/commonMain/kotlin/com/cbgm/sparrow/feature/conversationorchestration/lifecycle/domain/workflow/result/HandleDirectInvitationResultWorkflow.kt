package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.result

import com.cbgm.sparrow.feature.contacts.domain.usecase.BlockContactUseCase
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.AcceptIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DeclineIdentityExchangeUseCase
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResultAction
import kotlin.reflect.KClass

/**
 * incoming, accepted: AcceptExchange -> ActivateConversation
 * incoming, declined: DeclineExchange -> [BlockContact]
 * outgoing, declined: DiscardWaitingMessages
 *
 * Both identity setup modes create the receiver's chat and release its queued messages on acceptance.
 */
class HandleDirectInvitationResultWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val acceptIdentityExchange: AcceptIdentityExchangeUseCase,
    private val declineIdentityExchange: DeclineIdentityExchangeUseCase,
    private val blockContact: BlockContactUseCase
) : WorkflowStateMachine<
        LifecycleCommand.HandleDirectInvitationResult,
        HandleDirectInvitationResultWorkflow.Step,
        HandleDirectInvitationResultWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.HandleDirectInvitationResult> =
        LifecycleCommand.HandleDirectInvitationResult::class

    override fun firstStep(command: LifecycleCommand.HandleDirectInvitationResult): Step {
        val result = command.result
        return when {
            result.direction == InvitationDirection.INCOMING && result.response == InvitationResponse.ACCEPTED ->
                Step.AcceptExchange(command)
            result.direction == InvitationDirection.INCOMING && result.response == InvitationResponse.DECLINED ->
                Step.DeclineExchange(command)
            result.direction == InvitationDirection.OUTGOING && result.response == InvitationResponse.DECLINED ->
                Step.DiscardWaitingMessages(command)
            else -> Step.IgnoreResult(command)
        }
    }

    override suspend fun perform(step: Step): Outcome {
        val result = step.command.result
        return when (step) {
            is Step.AcceptExchange -> {
                acceptIdentityExchange(result.invitationId).getOrThrow()
                Outcome.ExchangeAccepted
            }
            is Step.ActivateConversation -> {
                engine.run(MessageCommand.ActivateAuthorizedConversation(result.peerId)).getOrThrow()
                Outcome.Done
            }
            is Step.DeclineExchange -> {
                declineIdentityExchange(result.invitationId).getOrThrow()
                Outcome.ExchangeDeclined
            }
            is Step.BlockContact -> {
                blockContact(result.peerId).getOrThrow()
                Outcome.Done
            }
            is Step.DiscardWaitingMessages -> {
                engine.run(MessageCommand.DiscardWaitingDirectMessages(result.peerId)).getOrThrow()
                Outcome.Done
            }
            is Step.IgnoreResult -> Outcome.Done
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.ExchangeAccepted -> Transition.Next(Step.ActivateConversation(step.command))
            Outcome.ExchangeDeclined ->
                if (step.command.result.action == InvitationResultAction.BLOCK_PEER) {
                    Transition.Next(Step.BlockContact(step.command))
                } else {
                    Transition.Complete()
                }
            Outcome.Done -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.HandleDirectInvitationResult

        data class AcceptExchange(
            override val command: LifecycleCommand.HandleDirectInvitationResult
        ) : Step

        data class ActivateConversation(
            override val command: LifecycleCommand.HandleDirectInvitationResult
        ) : Step

        data class DeclineExchange(
            override val command: LifecycleCommand.HandleDirectInvitationResult
        ) : Step

        data class BlockContact(
            override val command: LifecycleCommand.HandleDirectInvitationResult
        ) : Step

        data class DiscardWaitingMessages(
            override val command: LifecycleCommand.HandleDirectInvitationResult
        ) : Step

        /** Results that need no reaction, such as our own outgoing invitation being accepted. */
        data class IgnoreResult(
            override val command: LifecycleCommand.HandleDirectInvitationResult
        ) : Step
    }

    sealed interface Outcome {
        data object ExchangeAccepted : Outcome

        data object ExchangeDeclined : Outcome

        data object Done : Outcome
    }
}
