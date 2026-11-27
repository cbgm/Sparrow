package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupAvatarPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupAvatarUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlin.reflect.KClass

/** VerifySignature -> AuthorizeAdmin -> SaveAvatar. Older or unauthorized updates are dropped. */
class ReceiveGroupAvatarWorkflow internal constructor(
    private val packetCodec: GroupAvatarPacketCodec,
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val saveGroupAvatar: SaveGroupAvatarUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupAvatar,
        ReceiveGroupAvatarWorkflow.Step,
        ReceiveGroupAvatarWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupAvatar> = IncomingCommand.ReceiveGroupAvatar::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupAvatar): Step = Step.VerifySignature(command)

    override suspend fun perform(step: Step): Outcome {
        val packet = step.command.packet
        return when (step) {
            is Step.VerifySignature -> {
                packetCodec.verify(packet).getOrThrow()
                Outcome.SignatureValid
            }
            is Step.AuthorizeAdmin -> authorizeAdmin(step.command)
            is Step.SaveAvatar -> save(step.command)
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.SignatureValid -> Transition.Next(Step.AuthorizeAdmin(step.command))
            Outcome.AdminAuthorized -> Transition.Next(Step.SaveAvatar(step.command))
            Outcome.Ignored,
            Outcome.Saved -> Transition.Complete()
        }

    private suspend fun authorizeAdmin(command: IncomingCommand.ReceiveGroupAvatar): Outcome {
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

    /** Stores the version only while it is newer than the stored one, atomically against a local change. */
    private suspend fun save(command: IncomingCommand.ReceiveGroupAvatar): Outcome {
        val packet = command.packet
        val saved =
            saveGroupAvatar(
                GroupAvatarState(
                    groupId = packet.groupId,
                    bytes = packet.avatar.payload?.bytes,
                    changedAtEpochMilliseconds = packet.avatar.changedAtEpochMilliseconds
                )
            ).getOrThrow()
        return if (saved) Outcome.Saved else Outcome.Ignored
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupAvatar

        data class VerifySignature(
            override val command: IncomingCommand.ReceiveGroupAvatar
        ) : Step

        data class AuthorizeAdmin(
            override val command: IncomingCommand.ReceiveGroupAvatar
        ) : Step

        data class SaveAvatar(
            override val command: IncomingCommand.ReceiveGroupAvatar
        ) : Step
    }

    sealed interface Outcome {
        data object SignatureValid : Outcome

        data object AdminAuthorized : Outcome

        data object Ignored : Outcome

        data object Saved : Outcome
    }

    private companion object {
        const val ACTION = "avatar"
    }
}
