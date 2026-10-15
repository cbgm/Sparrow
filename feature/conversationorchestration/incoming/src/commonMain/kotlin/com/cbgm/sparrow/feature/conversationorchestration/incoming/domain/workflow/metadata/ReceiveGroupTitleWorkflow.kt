package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeGroupMetadataUseCase
import kotlin.reflect.KClass

/** VerifySignature -> AuthorizeAdmin -> LoadTitle -> SaveTitle. Older or unauthorized updates are dropped. */
class ReceiveGroupTitleWorkflow internal constructor(
    private val packetCodec: GroupTitlePacketCodec,
    private val authorizeGroupMetadata: AuthorizeGroupMetadataUseCase,
    private val titleRepository: GroupTitleRepository
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
            is Step.LoadTitle ->
                if (
                    packet.changedAtEpochMilliseconds > titleRepository.get(
                        packet.groupId
                    ).getOrThrow().changedAtEpochMilliseconds
                ) {
                    Outcome.UpdateIsNewer
                } else {
                    Outcome.Ignored
                }
            is Step.SaveTitle -> {
                titleRepository.save(
                    GroupTitleState(packet.groupId, packet.title, packet.changedAtEpochMilliseconds)
                ).getOrThrow()
                Outcome.Saved
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.SignatureValid -> Transition.Next(Step.AuthorizeAdmin(step.command))
            Outcome.AdminAuthorized -> Transition.Next(Step.LoadTitle(step.command))
            Outcome.UpdateIsNewer -> Transition.Next(Step.SaveTitle(step.command))
            Outcome.Ignored,
            Outcome.Saved -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupTitle

        data class VerifySignature(
            override val command: IncomingCommand.ReceiveGroupTitle
        ) : Step

        data class AuthorizeAdmin(
            override val command: IncomingCommand.ReceiveGroupTitle
        ) : Step

        data class LoadTitle(
            override val command: IncomingCommand.ReceiveGroupTitle
        ) : Step

        data class SaveTitle(
            override val command: IncomingCommand.ReceiveGroupTitle
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
        const val ACTION = "title"
    }
}
