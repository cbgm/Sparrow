package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupLeaveRequestUseCase
import kotlin.reflect.KClass

/** LoadContext -> ReceiveLeaveRequest -> ResolveMemberName -> RemoveParticipant -> RefreshVerification. */
class ReceiveGroupLeaveRequestWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: GroupConversationRepository,
    private val receiveGroupLeaveRequest: ReceiveGroupLeaveRequestUseCase,
    private val findMemberDisplayName: FindMemberDisplayNameUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupLeaveRequest,
        ReceiveGroupLeaveRequestWorkflow.Step,
        ReceiveGroupLeaveRequestWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupLeaveRequest> =
        IncomingCommand.ReceiveGroupLeaveRequest::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupLeaveRequest): Step = Step.LoadContext(command)

    override suspend fun perform(step: Step): Outcome {
        val packet = step.command.packet
        return when (step) {
            is Step.LoadContext ->
                Outcome.ContextLoaded(conversationRepository.getMembershipContext(packet.groupId).getOrThrow())
            is Step.ReceiveLeaveRequest ->
                Outcome.MemberLeft(
                    receiveGroupLeaveRequest(step.command.context.contactId, packet, step.context).getOrThrow()
                )
            is Step.ResolveMemberName ->
                Outcome.MemberNameResolved(
                    GroupParticipantRemoval(step.removal, findMemberDisplayName(step.removal.contactId).getOrThrow())
                )
            is Step.RemoveParticipant -> {
                conversationRepository.removeParticipant(step.removal).getOrThrow()
                Outcome.ParticipantRemoved(step.removal.result.groupId)
            }
            is Step.RefreshVerification -> {
                engine.run(MembershipCommand.RefreshOwnedGroupVerification(step.groupId)).getOrThrow()
                Outcome.VerificationRefreshed
            }
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContextLoaded -> Transition.Next(Step.ReceiveLeaveRequest(outcome.context, step.command))
            is Outcome.MemberLeft -> Transition.Next(Step.ResolveMemberName(outcome.removal, step.command))
            is Outcome.MemberNameResolved -> Transition.Next(Step.RemoveParticipant(outcome.removal, step.command))
            is Outcome.ParticipantRemoved -> Transition.Next(Step.RefreshVerification(outcome.groupId, step.command))
            Outcome.VerificationRefreshed -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupLeaveRequest

        data class LoadContext(
            override val command: IncomingCommand.ReceiveGroupLeaveRequest
        ) : Step

        data class ReceiveLeaveRequest(
            val context: GroupMembershipContext,
            override val command: IncomingCommand.ReceiveGroupLeaveRequest
        ) : Step

        data class ResolveMemberName(
            val removal: GroupMemberRemovalResult,
            override val command: IncomingCommand.ReceiveGroupLeaveRequest
        ) : Step

        data class RemoveParticipant(
            val removal: GroupParticipantRemoval,
            override val command: IncomingCommand.ReceiveGroupLeaveRequest
        ) : Step

        data class RefreshVerification(
            val groupId: String,
            override val command: IncomingCommand.ReceiveGroupLeaveRequest
        ) : Step
    }

    sealed interface Outcome {
        data class ContextLoaded(
            val context: GroupMembershipContext
        ) : Outcome

        data class MemberLeft(
            val removal: GroupMemberRemovalResult
        ) : Outcome

        data class MemberNameResolved(
            val removal: GroupParticipantRemoval
        ) : Outcome

        data class ParticipantRemoved(
            val groupId: String
        ) : Outcome

        data object VerificationRefreshed : Outcome
    }
}
