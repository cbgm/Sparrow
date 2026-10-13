package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeBindingUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.ReceiveIdentityReadyUseCase
import com.cbgm.sparrow.protocol.invitation.ContactInvitationHandshakeProtocol
import kotlin.reflect.KClass

/** FindExchange -> VerifyReady -> ReceiveReady. READY completes the inviter's side of the exchange. */
class ReceiveContactReadyWorkflow internal constructor(
    private val getIdentityExchangeBinding: GetIdentityExchangeBindingUseCase,
    private val invitationHandshakeProtocol: ContactInvitationHandshakeProtocol,
    private val receiveIdentityReady: ReceiveIdentityReadyUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveContactReady,
        ReceiveContactReadyWorkflow.Step,
        ReceiveContactReadyWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveContactReady> = IncomingCommand.ReceiveContactReady::class

    override fun firstStep(command: IncomingCommand.ReceiveContactReady): Step = Step.FindExchange(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.FindExchange -> {
                val binding =
                    getIdentityExchangeBinding(packet.invitationId).getOrThrow() ?: error(
                        "Exchange was not found: ${packet.invitationId}"
                    )
                check(binding.peerId == context.contactId) { "Ready sender does not match exchange" }
                Outcome.ExchangeFound(binding)
            }
            is Step.VerifyReady -> {
                invitationHandshakeProtocol.verifyReady(
                    packet,
                    step.binding.remoteSigningPublicKey,
                    context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.ReadyVerified
            }
            is Step.ReceiveReady -> {
                receiveIdentityReady(
                    context,
                    IdentityExchangeReady(
                        exchangeId = packet.invitationId,
                        responseChallenge = packet.responseChallenge,
                        acceptedResponderEncryptionPublicKey = packet.acceptedResponderEncryptionPublicKey,
                        acceptedResponderSigningPublicKey = packet.acceptedResponderSigningPublicKey,
                        senderEncryptionPublicKey = packet.senderEncryptionPublicKey,
                        senderSigningPublicKey = packet.senderSigningPublicKey
                    )
                ).getOrThrow()
                Outcome.ReadyReceived
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ExchangeFound -> Transition.Next(Step.VerifyReady(outcome.binding, step.command))
            Outcome.ReadyVerified -> Transition.Next(Step.ReceiveReady(step.command))
            Outcome.ReadyReceived -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveContactReady

        data class FindExchange(
            override val command: IncomingCommand.ReceiveContactReady
        ) : Step

        data class VerifyReady(
            val binding: IdentityExchangeBinding,
            override val command: IncomingCommand.ReceiveContactReady
        ) : Step

        data class ReceiveReady(
            override val command: IncomingCommand.ReceiveContactReady
        ) : Step
    }

    sealed interface Outcome {
        data class ExchangeFound(
            val binding: IdentityExchangeBinding
        ) : Outcome

        data object ReadyVerified : Outcome

        data object ReadyReceived : Outcome
    }
}
