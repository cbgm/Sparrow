package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.recovery

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.command.MessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosurePhase
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeClosureUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import kotlin.reflect.KClass

/**
 * LoadAuthorization -> FindConversation -> ActivateConversation
 * LoadAuthorization -> FindConversation -> CheckExchangeStillActive -> ActivateConversation
 *
 * An established exchange can be persisted right before a process death, without its chat. Only the
 * exact, still active exchange may create the missing chat; a deliberately deleted conversation is never
 * resurrected from history. An existing conversation keeps its ID and messages.
 */
class RecoverAuthorizedConversationWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val getIdentityPeerState: GetIdentityPeerStateUseCase,
    private val conversationRepository: DirectConversationRepository,
    private val getIdentityExchangeClosure: GetIdentityExchangeClosureUseCase
) : WorkflowStateMachine<
        LifecycleCommand.RecoverAuthorizedConversation,
        RecoverAuthorizedConversationWorkflow.Step,
        RecoverAuthorizedConversationWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.RecoverAuthorizedConversation> =
        LifecycleCommand.RecoverAuthorizedConversation::class

    override fun firstStep(command: LifecycleCommand.RecoverAuthorizedConversation): Step =
        Step.LoadAuthorization(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadAuthorization ->
                if (step.command.result.status == IdentityResultStatus.ESTABLISHED &&
                    getIdentityPeerState(step.command.result.peerId).getOrThrow().hasEstablishedExchange
                ) {
                    Outcome.Authorized
                } else {
                    Outcome.NothingToRecover
                }
            is Step.FindConversation ->
                if (conversationRepository.findConversationId(step.command.result.peerId).getOrThrow() != null) {
                    Outcome.ConversationExists
                } else {
                    Outcome.ConversationMissing
                }
            is Step.CheckExchangeStillActive -> {
                val current = getIdentityExchangeClosure(step.command.result.peerId).getOrThrow()
                if (current?.exchangeId == step.command.result.exchangeId &&
                    current.phase == IdentityExchangeClosurePhase.ACTIVE
                ) {
                    Outcome.ExchangeStillActive
                } else {
                    Outcome.NothingToRecover
                }
            }
            is Step.ActivateConversation -> {
                engine.run(
                    MessageCommand.ActivateAuthorizedConversation(step.command.result.peerId)
                ).getOrThrow()
                Outcome.ConversationActivated
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.Authorized -> Transition.Next(Step.FindConversation(step.command))
            Outcome.NothingToRecover -> Transition.Complete()
            Outcome.ConversationExists -> Transition.Next(Step.ActivateConversation(step.command))
            Outcome.ConversationMissing -> Transition.Next(Step.CheckExchangeStillActive(step.command))
            Outcome.ExchangeStillActive -> Transition.Next(Step.ActivateConversation(step.command))
            Outcome.ConversationActivated -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.RecoverAuthorizedConversation

        data class LoadAuthorization(
            override val command: LifecycleCommand.RecoverAuthorizedConversation
        ) : Step

        data class FindConversation(
            override val command: LifecycleCommand.RecoverAuthorizedConversation
        ) : Step

        data class CheckExchangeStillActive(
            override val command: LifecycleCommand.RecoverAuthorizedConversation
        ) : Step

        data class ActivateConversation(
            override val command: LifecycleCommand.RecoverAuthorizedConversation
        ) : Step
    }

    sealed interface Outcome {
        data object Authorized : Outcome

        data object NothingToRecover : Outcome

        data object ConversationExists : Outcome

        data object ConversationMissing : Outcome

        data object ExchangeStillActive : Outcome

        data object ConversationActivated : Outcome
    }
}
