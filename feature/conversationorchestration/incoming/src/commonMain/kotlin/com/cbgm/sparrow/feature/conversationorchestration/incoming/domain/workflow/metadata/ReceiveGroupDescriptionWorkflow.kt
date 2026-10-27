package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupDescriptionPacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CheckIncomingGroupStateVersionUseCase
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlin.reflect.KClass

/** VerifySignature -> AuthorizeAdmin -> LoadDescription -> SaveDescription. Older or unauthorized updates are dropped. */
class ReceiveGroupDescriptionWorkflow internal constructor(
    private val packetCodec: GroupDescriptionPacketCodec,
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val descriptionRepository: GroupDescriptionRepository,
    private val checkIncomingGroupStateVersion: CheckIncomingGroupStateVersionUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupDescription,
        ReceiveGroupDescriptionWorkflow.Step,
        ReceiveGroupDescriptionWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupDescription> =
        IncomingCommand.ReceiveGroupDescription::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupDescription): Step = Step.VerifySignature(command)

    override suspend fun perform(step: Step): Outcome {
        val packet = step.command.packet
        return when (step) {
            is Step.VerifySignature -> {
                packetCodec.verify(packet).getOrThrow()
                Outcome.SignatureValid
            }
            is Step.AuthorizeAdmin -> authorizeAdmin(step.command)
            is Step.LoadDescription -> checkNewer(step.command)
            is Step.SaveDescription -> {
                descriptionRepository.save(
                    GroupDescriptionState(packet.groupId, packet.description, packet.changedAtEpochMilliseconds)
                ).getOrThrow()
                Outcome.Saved
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.SignatureValid -> Transition.Next(Step.AuthorizeAdmin(step.command))
            Outcome.AdminAuthorized -> Transition.Next(Step.LoadDescription(step.command))
            Outcome.UpdateIsNewer -> Transition.Next(Step.SaveDescription(step.command))
            Outcome.Ignored,
            Outcome.Saved -> Transition.Complete()
        }

    private suspend fun authorizeAdmin(command: IncomingCommand.ReceiveGroupDescription): Outcome {
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

    private suspend fun checkNewer(command: IncomingCommand.ReceiveGroupDescription): Outcome {
        val packet = command.packet
        return if (
            checkIncomingGroupStateVersion.isNewerDescription(packet.groupId, packet.changedAtEpochMilliseconds).getOrThrow()
        ) {
            Outcome.UpdateIsNewer
        } else {
            Outcome.Ignored
        }
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupDescription

        data class VerifySignature(
            override val command: IncomingCommand.ReceiveGroupDescription
        ) : Step

        data class AuthorizeAdmin(
            override val command: IncomingCommand.ReceiveGroupDescription
        ) : Step

        data class LoadDescription(
            override val command: IncomingCommand.ReceiveGroupDescription
        ) : Step

        data class SaveDescription(
            override val command: IncomingCommand.ReceiveGroupDescription
        ) : Step
    }

    sealed interface Outcome {
        data object SignatureValid : Outcome

        data object AdminAuthorized : Outcome

        data object UpdateIsNewer : Outcome

        data object Ignored : Outcome

        data object Saved : Outcome
    }

    private companion object {
        const val ACTION = "description"
    }
}
