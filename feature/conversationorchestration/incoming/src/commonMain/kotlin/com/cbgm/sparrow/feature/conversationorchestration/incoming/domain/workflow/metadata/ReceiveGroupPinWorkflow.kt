package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupPinPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.identity.domain.usecase.FindRemoteIdentityPeerIdUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlin.reflect.KClass

/**
 * VerifySignature -> AuthorizeAdmin -> CheckPinSlot -> [ResolveMessageSender] -> SavePin
 *
 * Older updates are dropped, and an active expense board keeps the pin slot against other messages.
 */
class ReceiveGroupPinWorkflow internal constructor(
    private val packetCodec: GroupPinPacketCodec,
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val pinRepository: GroupPinRepository,
    private val findRemoteIdentityPeerId: FindRemoteIdentityPeerIdUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupPin,
        ReceiveGroupPinWorkflow.Step,
        ReceiveGroupPinWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupPin> = IncomingCommand.ReceiveGroupPin::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupPin): Step = Step.VerifySignature(command)

    override suspend fun perform(step: Step): Outcome {
        val packet = step.command.packet
        return when (step) {
            is Step.VerifySignature -> {
                packetCodec.verify(packet).getOrThrow()
                Outcome.SignatureValid
            }
            is Step.AuthorizeAdmin ->
                if (
                    authorizeGroupMetadata.receive(
                        groupId = packet.groupId,
                        epoch = packet.epoch,
                        contactId = step.command.context.contactId,
                        adminSigningPublicKey = packet.adminSigningPublicKey,
                        action = ACTION
                    ).getOrThrow()
                ) {
                    Outcome.AdminAuthorized
                } else {
                    Outcome.Ignored
                }
            is Step.CheckPinSlot -> {
                val current = pinRepository.find(packet.groupId).getOrThrow()
                val newer = packet.changedAtEpochMilliseconds > (current?.changedAtEpochMilliseconds ?: 0L)
                val expensesKeepSlot =
                    current?.hasActiveExpenseBoard == true && packet.messageId != null && packet.messageId != current.messageId
                when {
                    !newer || expensesKeepSlot -> Outcome.Ignored
                    packet.hasPinnedMessage -> Outcome.SlotAvailable
                    else -> Outcome.SenderResolved(senderContactId = null, isMine = false)
                }
            }
            is Step.ResolveMessageSender -> {
                val sender =
                    authorizeGroupMetadata.resolveMessageSender(
                        packet.groupId,
                        packet.epoch,
                        packet.messageSenderSigningPublicKey
                    )
                        .getOrThrow()
                val senderContactId =
                    if (sender.isLocal) {
                        null
                    } else {
                        sender.memberContactId ?: findRemoteIdentityPeerId(
                            packet.messageSenderSigningPublicKey
                        ).getOrThrow()
                    }
                Outcome.SenderResolved(senderContactId, sender.isLocal)
            }
            is Step.SavePin -> {
                pinRepository.save(packetCodec.toGroupPinState(packet, step.senderContactId, step.isMine)).getOrThrow()
                Outcome.Saved
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.SignatureValid -> Transition.Next(Step.AuthorizeAdmin(step.command))
            Outcome.AdminAuthorized -> Transition.Next(Step.CheckPinSlot(step.command))
            Outcome.SlotAvailable -> Transition.Next(Step.ResolveMessageSender(step.command))
            is Outcome.SenderResolved ->
                Transition.Next(Step.SavePin(outcome.senderContactId, outcome.isMine, step.command))
            Outcome.Ignored,
            Outcome.Saved -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupPin

        data class VerifySignature(
            override val command: IncomingCommand.ReceiveGroupPin
        ) : Step

        data class AuthorizeAdmin(
            override val command: IncomingCommand.ReceiveGroupPin
        ) : Step

        data class CheckPinSlot(
            override val command: IncomingCommand.ReceiveGroupPin
        ) : Step

        data class ResolveMessageSender(
            override val command: IncomingCommand.ReceiveGroupPin
        ) : Step

        data class SavePin(
            val senderContactId: String?,
            val isMine: Boolean,
            override val command: IncomingCommand.ReceiveGroupPin
        ) : Step
    }

    sealed interface Outcome {
        data object SignatureValid : Outcome

        data object AdminAuthorized : Outcome

        /** The pin names a message whose sender must be resolved. */
        data object SlotAvailable : Outcome

        data class SenderResolved(
            val senderContactId: String?,
            val isMine: Boolean
        ) : Outcome

        data object Ignored : Outcome

        data object Saved : Outcome
    }

    private companion object {
        const val ACTION = "pin"
    }
}
