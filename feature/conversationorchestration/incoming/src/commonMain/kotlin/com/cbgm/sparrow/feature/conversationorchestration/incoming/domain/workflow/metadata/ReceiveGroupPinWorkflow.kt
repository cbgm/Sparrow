package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupPinPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CheckIncomingGroupStateVersionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.ResolveIncomingPinSenderUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlin.reflect.KClass

/**
 * VerifySignature -> AuthorizeAdmin -> CheckPinSlot -> [ResolveMessageSender] -> SavePin
 *
 * Older updates are dropped, and an active expense board keeps the pin slot against other messages. SavePin checks
 * both again atomically with the save, because a local pin may be saved while the sender is resolved.
 */
class ReceiveGroupPinWorkflow internal constructor(
    private val packetCodec: GroupPinPacketCodec,
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val checkIncomingGroupStateVersion: CheckIncomingGroupStateVersionUseCase,
    private val resolveIncomingPinSender: ResolveIncomingPinSenderUseCase,
    private val saveGroupPin: SaveGroupPinUseCase
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
            is Step.AuthorizeAdmin -> authorizeAdmin(step.command)
            is Step.CheckPinSlot -> checkPinSlot(step.command)
            is Step.ResolveMessageSender -> {
                val sender =
                    resolveIncomingPinSender(
                        packet.groupId,
                        packet.epoch,
                        packet.messageSenderSigningPublicKey
                    ).getOrThrow()
                Outcome.SenderResolved(sender.memberContactId, sender.isLocal)
            }
            is Step.SavePin -> savePin(step)
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

    private suspend fun authorizeAdmin(command: IncomingCommand.ReceiveGroupPin): Outcome {
        val packet = command.packet
        return if (
            authorizeGroupMetadata.receive(
                groupId = packet.groupId,
                epoch = packet.epoch,
                contactId = command.context.contactId,
                adminSigningPublicKey = packet.adminSigningPublicKey,
                action = ACTION
            ).getOrThrow()
        ) {
            Outcome.AdminAuthorized
        } else {
            Outcome.Ignored
        }
    }

    /** A cleared slot needs no message sender. */
    private suspend fun checkPinSlot(command: IncomingCommand.ReceiveGroupPin): Outcome {
        val packet = command.packet
        return when {
            !checkIncomingGroupStateVersion.acceptsPin(
                packet.groupId,
                packet.changedAtEpochMilliseconds,
                packet.messageId
            ).getOrThrow() -> Outcome.Ignored
            packet.hasPinnedMessage -> Outcome.SlotAvailable
            else -> Outcome.SenderResolved(senderContactId = null, isMine = false)
        }
    }

    private suspend fun savePin(step: Step.SavePin): Outcome {
        val pin = packetCodec.toGroupPinState(step.command.packet, step.senderContactId, step.isMine)
        return if (saveGroupPin.received(pin).getOrThrow()) Outcome.Saved else Outcome.Ignored
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
