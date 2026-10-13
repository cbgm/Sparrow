package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupIncomingConversationRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupRemovalUseCase
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import kotlin.reflect.KClass

/**
 * [LoadPendingOwnerKey] -> AuthorizeRemoval -> ApplyRemoval -> CompleteRemoval
 *
 * The local member was removed or left. A pending invitation's removal is signed with the owner's
 * contact identity; an established membership's with its epoch keys.
 */
class ReceiveGroupMemberRemovalWorkflow internal constructor(
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val authorizeIncomingGroupRemoval: AuthorizeIncomingGroupRemovalUseCase,
    private val incomingConversationRepository: GroupIncomingConversationRepository,
    private val completeIncomingGroupRemoval: CompleteIncomingGroupRemovalUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupMemberRemoval,
        ReceiveGroupMemberRemovalWorkflow.Step,
        ReceiveGroupMemberRemovalWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupMemberRemoval> =
        IncomingCommand.ReceiveGroupMemberRemoval::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupMemberRemoval): Step =
        if (command.packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
            Step.LoadPendingOwnerKey(command)
        } else {
            Step.AuthorizeRemoval(pendingOwnerSigningPublicKey = null, command = command)
        }

    override suspend fun perform(step: Step): Outcome {
        val context = step.command.context
        val packet = step.command.packet
        return when (step) {
            is Step.LoadPendingOwnerKey ->
                Outcome.PendingOwnerKeyLoaded(
                    getRemoteIdentity(context.contactId).getOrThrow()?.signingPublicKey
                        ?: error("Pending group membership owner identity was not found")
                )
            is Step.AuthorizeRemoval ->
                if (
                    authorizeIncomingGroupRemoval(
                        packet,
                        context.contactId,
                        step.pendingOwnerSigningPublicKey
                    ).getOrThrow()
                ) {
                    Outcome.RemovalAuthorized
                } else {
                    Outcome.NotApplicable
                }
            is Step.ApplyRemoval -> {
                incomingConversationRepository.applyRemoval(packet).getOrThrow()
                Outcome.RemovalApplied
            }
            is Step.CompleteRemoval -> {
                completeIncomingGroupRemoval(packet).getOrThrow()
                Outcome.Completed
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.PendingOwnerKeyLoaded ->
                Transition.Next(Step.AuthorizeRemoval(outcome.signingPublicKey, step.command))
            Outcome.RemovalAuthorized -> Transition.Next(Step.ApplyRemoval(step.command))
            Outcome.RemovalApplied -> Transition.Next(Step.CompleteRemoval(step.command))
            Outcome.NotApplicable,
            Outcome.Completed -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupMemberRemoval

        data class LoadPendingOwnerKey(
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step

        class AuthorizeRemoval(
            val pendingOwnerSigningPublicKey: ByteArray?,
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step

        data class ApplyRemoval(
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step

        data class CompleteRemoval(
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step
    }

    sealed interface Outcome {
        class PendingOwnerKeyLoaded(
            val signingPublicKey: ByteArray
        ) : Outcome

        data object RemovalAuthorized : Outcome

        data object NotApplicable : Outcome

        data object RemovalApplied : Outcome

        data object Completed : Outcome
    }
}
