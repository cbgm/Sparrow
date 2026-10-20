package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.identity

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.IncomingAuthorizationRevocationDecision
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.decideIncomingAuthorizationRevocation
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityExchangeBindingUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.InvalidateIdentityExchangeUseCase
import com.cbgm.sparrow.protocol.authorization.DirectChatAuthorizationRevocationProtocol
import kotlin.reflect.KClass

/**
 * FindExchange -> VerifySignature -> DecideRevocation -> InvalidateExchange
 *
 * The signature is verified before the signer is compared with the invitation's identity: a self-signed
 * packet from a new identity never revokes the previous identity's chat. It is acknowledged and ignored.
 */
class ReceiveAuthorizationRevokedWorkflow internal constructor(
    private val getIdentityExchangeBinding: GetIdentityExchangeBindingUseCase,
    private val revocationProtocol: DirectChatAuthorizationRevocationProtocol,
    private val invalidateIdentityExchange: InvalidateIdentityExchangeUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveAuthorizationRevoked,
        ReceiveAuthorizationRevokedWorkflow.Step,
        ReceiveAuthorizationRevokedWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("ReceiveAuthorizationRevokedWorkflow")

    override val commandType: KClass<IncomingCommand.ReceiveAuthorizationRevoked> =
        IncomingCommand.ReceiveAuthorizationRevoked::class

    override fun firstStep(command: IncomingCommand.ReceiveAuthorizationRevoked): Step =
        Step.FindExchange(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.FindExchange ->
                Outcome.ExchangeFound(
                    getIdentityExchangeBinding(packet.invitationId).getOrThrow() ?: error(
                        "Invitation was not found: ${packet.invitationId}"
                    )
                )
            is Step.VerifySignature -> {
                revocationProtocol.verifyPacket(packet).getOrThrow()
                Outcome.SignatureValid(step.binding)
            }
            is Step.DecideRevocation ->
                when (decideIncomingAuthorizationRevocation(context, packet, step.binding)) {
                    IncomingAuthorizationRevocationDecision.APPLY -> Outcome.RevocationApplies
                    IncomingAuthorizationRevocationDecision.IGNORE_UNRECOGNIZED_SIGNER -> {
                        logger.info {
                            "Ignoring authorization revocation with a signing key that does not match the stored invitation identity: " +
                                "invitationId=${packet.invitationId}"
                        }
                        Outcome.Done
                    }
                }
            is Step.InvalidateExchange -> {
                invalidateIdentityExchange(
                    exchangeId = packet.invitationId,
                    peerId = context.contactId,
                    expectedChallenge = packet.inviteChallenge,
                    expectedSigningPublicKey = packet.revokerSigningPublicKey,
                    atEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.Done
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ExchangeFound -> Transition.Next(Step.VerifySignature(outcome.binding, step.command))
            is Outcome.SignatureValid -> Transition.Next(Step.DecideRevocation(outcome.binding, step.command))
            Outcome.RevocationApplies -> Transition.Next(Step.InvalidateExchange(step.command))
            Outcome.Done -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveAuthorizationRevoked

        data class FindExchange(
            override val command: IncomingCommand.ReceiveAuthorizationRevoked
        ) : Step

        data class VerifySignature(
            val binding: IdentityExchangeBinding,
            override val command: IncomingCommand.ReceiveAuthorizationRevoked
        ) : Step

        data class DecideRevocation(
            val binding: IdentityExchangeBinding,
            override val command: IncomingCommand.ReceiveAuthorizationRevoked
        ) : Step

        data class InvalidateExchange(
            override val command: IncomingCommand.ReceiveAuthorizationRevoked
        ) : Step
    }

    sealed interface Outcome {
        data class ExchangeFound(
            val binding: IdentityExchangeBinding
        ) : Outcome

        data class SignatureValid(
            val binding: IdentityExchangeBinding
        ) : Outcome

        data object RevocationApplies : Outcome

        data object Done : Outcome
    }
}
