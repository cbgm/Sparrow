package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupTitleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlin.reflect.KClass

/** VerifySignature -> AuthorizeAdmin -> SaveTitle. Older or unauthorized updates are dropped. */
class ReceiveGroupTitleWorkflow internal constructor(
    private val packetCodec: GroupTitlePacketCodec,
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val saveGroupTitle: SaveGroupTitleUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupTitle,
        ReceiveGroupTitleWorkflow.Step,
        ReceiveGroupTitleWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupTitle> = IncomingCommand.ReceiveGroupTitle::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupTitle): Step = Step.VerifySignature(command)

    override suspend fun perform(step: Step): Outcome {
        val packet = step.command.packet
        return when (step) {
            is Step.VerifySignature -> {
                packetCodec.verify(packet).getOrThrow()
                Outcome.SignatureValid
            }
            is Step.AuthorizeAdmin -> authorizeAdmin(step.command)
            is Step.SaveTitle -> save(step.command)
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.SignatureValid -> Transition.Next(Step.AuthorizeAdmin(step.command))
            Outcome.AdminAuthorized -> Transition.Next(Step.SaveTitle(step.command))
            Outcome.Ignored,
            Outcome.Saved -> Transition.Complete()
        }

    private suspend fun authorizeAdmin(command: IncomingCommand.ReceiveGroupTitle): Outcome {
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
    private suspend fun save(command: IncomingCommand.ReceiveGroupTitle): Outcome {
        val packet = command.packet
        val saved =
            saveGroupTitle(
                GroupTitleState(packet.groupId, packet.title, packet.changedAtEpochMilliseconds)
            ).getOrThrow()
        return if (saved) Outcome.Saved else Outcome.Ignored
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupTitle

        data class VerifySignature(
            override val command: IncomingCommand.ReceiveGroupTitle
        ) : Step

        data class AuthorizeAdmin(
            override val command: IncomingCommand.ReceiveGroupTitle
        ) : Step

        data class SaveTitle(
            override val command: IncomingCommand.ReceiveGroupTitle
        ) : Step
    }

    sealed interface Outcome {
        data object SignatureValid : Outcome

        data object AdminAuthorized : Outcome

        data object Ignored : Outcome

        data object Saved : Outcome
    }

    private companion object {
        const val ACTION = "title"
    }
}
