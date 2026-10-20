package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupIncomingConversationRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupDeletionUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupDeletionUseCase
import kotlin.reflect.KClass

/** LoadOwnerKey -> AuthorizeDeletion -> DeleteAttachments -> PrepareDeletion -> CompleteDeletion -> FinishDeletion. */
class ReceiveGroupDeletionWorkflow internal constructor(
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val authorizeIncomingGroupDeletion: AuthorizeIncomingGroupDeletionUseCase,
    private val attachmentRepository: MessageAttachmentRepository,
    private val incomingConversationRepository: GroupIncomingConversationRepository,
    private val completeIncomingGroupDeletion: CompleteIncomingGroupDeletionUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupDeletion,
        ReceiveGroupDeletionWorkflow.Step,
        ReceiveGroupDeletionWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupDeletion> = IncomingCommand.ReceiveGroupDeletion::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupDeletion): Step = Step.LoadOwnerKey(command)

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.LoadOwnerKey ->
                Outcome.OwnerKeyLoaded(
                    getRemoteIdentity(context.contactId).getOrThrow()?.signingPublicKey ?: error(
                        "Group owner identity was not found"
                    )
                )
            is Step.AuthorizeDeletion -> {
                authorizeIncomingGroupDeletion(packet, context.contactId, step.ownerSigningPublicKey).getOrThrow()
                Outcome.DeletionAuthorized
            }
            is Step.DeleteAttachments -> {
                attachmentRepository.deleteLocalAttachmentsForConversation(packet.groupId).getOrThrow()
                Outcome.AttachmentsDeleted
            }
            is Step.PrepareDeletion -> {
                incomingConversationRepository.prepareDeletion(packet.groupId).getOrThrow()
                Outcome.DeletionPrepared
            }
            is Step.CompleteDeletion -> {
                completeIncomingGroupDeletion(packet, context.contactId).getOrThrow()
                Outcome.MembershipDeleted
            }
            is Step.FinishDeletion -> {
                incomingConversationRepository.finishDeletion(
                    packet.groupId,
                    packet.deletedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.Finished
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.OwnerKeyLoaded -> Transition.Next(Step.AuthorizeDeletion(outcome.signingPublicKey, step.command))
            Outcome.DeletionAuthorized -> Transition.Next(Step.DeleteAttachments(step.command))
            Outcome.AttachmentsDeleted -> Transition.Next(Step.PrepareDeletion(step.command))
            Outcome.DeletionPrepared -> Transition.Next(Step.CompleteDeletion(step.command))
            Outcome.MembershipDeleted -> Transition.Next(Step.FinishDeletion(step.command))
            Outcome.Finished -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupDeletion

        data class LoadOwnerKey(
            override val command: IncomingCommand.ReceiveGroupDeletion
        ) : Step

        class AuthorizeDeletion(
            val ownerSigningPublicKey: ByteArray,
            override val command: IncomingCommand.ReceiveGroupDeletion
        ) : Step

        data class DeleteAttachments(
            override val command: IncomingCommand.ReceiveGroupDeletion
        ) : Step

        data class PrepareDeletion(
            override val command: IncomingCommand.ReceiveGroupDeletion
        ) : Step

        data class CompleteDeletion(
            override val command: IncomingCommand.ReceiveGroupDeletion
        ) : Step

        data class FinishDeletion(
            override val command: IncomingCommand.ReceiveGroupDeletion
        ) : Step
    }

    sealed interface Outcome {
        class OwnerKeyLoaded(
            val signingPublicKey: ByteArray
        ) : Outcome

        data object DeletionAuthorized : Outcome

        data object AttachmentsDeleted : Outcome

        data object DeletionPrepared : Outcome

        data object MembershipDeleted : Outcome

        data object Finished : Outcome
    }
}
