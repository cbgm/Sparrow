package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.workflow.direct

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosurePhase
import com.cbgm.sparrow.feature.identity.domain.usecase.CloseIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.DeclineIdentityExchangeUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeClosureUseCase
import com.cbgm.sparrow.protocol.authorization.DirectChatAuthorizationRevocationSender
import kotlin.reflect.KClass

/**
 * FindExchange -> DeclineExchange -> CloseExchange   (an incoming invitation is still pending)
 * FindExchange -> SendRevocation -> CloseExchange    (the exchange is active)
 * FindExchange -> CloseExchange                      (the exchange is terminal)
 *
 * Only orchestration decides whether to decline, revoke or just close the Identity-owned exchange.
 */
class RevokePeerExchangeWorkflow internal constructor(
    private val getIdentityExchangeClosure: GetIdentityExchangeClosureUseCase,
    private val declineIdentityExchange: DeclineIdentityExchangeUseCase,
    private val revocationSender: DirectChatAuthorizationRevocationSender,
    private val closeIdentityExchange: CloseIdentityExchangeUseCase
) : WorkflowStateMachine<
        LifecycleCommand.RevokePeerExchange,
        RevokePeerExchangeWorkflow.Step,
        RevokePeerExchangeWorkflow.Outcome
    > {
    override val commandType: KClass<LifecycleCommand.RevokePeerExchange> = LifecycleCommand.RevokePeerExchange::class

    override fun firstStep(command: LifecycleCommand.RevokePeerExchange): Step = Step.FindExchange(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindExchange ->
                getIdentityExchangeClosure(step.command.contactId).getOrThrow()
                    ?.let(Outcome::ExchangeFound)
                    ?: Outcome.NoExchange
            is Step.DeclineExchange -> {
                declineIdentityExchange(step.exchange.exchangeId).getOrThrow()
                Outcome.ExchangeEnded(step.exchange)
            }
            is Step.SendRevocation -> {
                revocationSender.enqueueOrResend(
                    peerId = step.exchange.peerId,
                    exchangeId = step.exchange.exchangeId,
                    inviteChallenge = step.exchange.inviteChallenge
                ).getOrThrow()
                Outcome.ExchangeEnded(step.exchange)
            }
            is Step.CloseExchange -> {
                closeIdentityExchange(step.exchange.exchangeId, step.command.contactId).getOrThrow()
                Outcome.ExchangeClosed
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NoExchange -> Transition.Complete()
            is Outcome.ExchangeFound ->
                when (outcome.exchange.phase) {
                    IdentityExchangeClosurePhase.INCOMING_PENDING ->
                        Transition.Next(Step.DeclineExchange(outcome.exchange, step.command))
                    IdentityExchangeClosurePhase.ACTIVE ->
                        Transition.Next(Step.SendRevocation(outcome.exchange, step.command))
                    IdentityExchangeClosurePhase.TERMINAL ->
                        Transition.Next(Step.CloseExchange(outcome.exchange, step.command))
                    IdentityExchangeClosurePhase.ALREADY_CLOSED -> Transition.Complete()
                }
            is Outcome.ExchangeEnded -> Transition.Next(Step.CloseExchange(outcome.exchange, step.command))
            Outcome.ExchangeClosed -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: LifecycleCommand.RevokePeerExchange

        data class FindExchange(
            override val command: LifecycleCommand.RevokePeerExchange
        ) : Step

        data class DeclineExchange(
            val exchange: IdentityExchangeClosure,
            override val command: LifecycleCommand.RevokePeerExchange
        ) : Step

        data class SendRevocation(
            val exchange: IdentityExchangeClosure,
            override val command: LifecycleCommand.RevokePeerExchange
        ) : Step

        data class CloseExchange(
            val exchange: IdentityExchangeClosure,
            override val command: LifecycleCommand.RevokePeerExchange
        ) : Step
    }

    sealed interface Outcome {
        data object NoExchange : Outcome

        data class ExchangeFound(
            val exchange: IdentityExchangeClosure
        ) : Outcome

        data class ExchangeEnded(
            val exchange: IdentityExchangeClosure
        ) : Outcome

        data object ExchangeClosed : Outcome
    }
}
