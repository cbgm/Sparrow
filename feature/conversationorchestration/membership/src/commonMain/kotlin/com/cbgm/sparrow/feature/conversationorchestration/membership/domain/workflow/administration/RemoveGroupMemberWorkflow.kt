package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import kotlin.reflect.KClass

/** LoadContext -> RemoveMember -> ResolveMemberName -> ProjectRemoval -> RefreshVerification. */
class RemoveGroupMemberWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: GroupConversationRepository,
    private val removeGroupMember: RemoveGroupMemberUseCase,
    private val findMemberDisplayName: FindMemberDisplayNameUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.RemoveMember,
        RemoveGroupMemberWorkflow.Step,
        RemoveGroupMemberWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.RemoveMember> =
        GroupAdministrationCommand.RemoveMember::class

    override fun lockKey(command: GroupAdministrationCommand.RemoveMember): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.RemoveMember): Step = Step.LoadContext(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadContext ->
                Outcome.ContextLoaded(conversationRepository.getMembershipContext(step.command.groupId).getOrThrow())
            is Step.RemoveMember ->
                Outcome.MemberRemoved(
                    removeGroupMember(step.command.groupId, step.command.contactId, step.context).getOrThrow()
                )
            is Step.ResolveMemberName ->
                Outcome.MemberNameResolved(
                    GroupParticipantRemoval(step.removal, findMemberDisplayName(step.removal.contactId).getOrThrow())
                )
            is Step.ProjectRemoval -> {
                conversationRepository.removeParticipant(step.removal).getOrThrow()
                Outcome.RemovalProjected
            }
            is Step.RefreshVerification -> {
                engine.run(
                    MembershipCommand.RefreshOwnedGroupVerification(step.command.groupId)
                ).getOrThrow()
                Outcome.VerificationRefreshed
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.ContextLoaded -> Transition.Next(Step.RemoveMember(outcome.context, step.command))
            is Outcome.MemberRemoved -> Transition.Next(Step.ResolveMemberName(outcome.removal, step.command))
            is Outcome.MemberNameResolved -> Transition.Next(Step.ProjectRemoval(outcome.removal, step.command))
            Outcome.RemovalProjected -> Transition.Next(Step.RefreshVerification(step.command))
            Outcome.VerificationRefreshed -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.RemoveMember

        data class LoadContext(
            override val command: GroupAdministrationCommand.RemoveMember
        ) : Step

        data class RemoveMember(
            val context: GroupMembershipContext,
            override val command: GroupAdministrationCommand.RemoveMember
        ) : Step

        data class ResolveMemberName(
            val removal: GroupMemberRemovalResult,
            override val command: GroupAdministrationCommand.RemoveMember
        ) : Step

        data class ProjectRemoval(
            val removal: GroupParticipantRemoval,
            override val command: GroupAdministrationCommand.RemoveMember
        ) : Step

        data class RefreshVerification(
            override val command: GroupAdministrationCommand.RemoveMember
        ) : Step
    }

    sealed interface Outcome {
        data class ContextLoaded(
            val context: GroupMembershipContext
        ) : Outcome

        data class MemberRemoved(
            val removal: GroupMemberRemovalResult
        ) : Outcome

        data class MemberNameResolved(
            val removal: GroupParticipantRemoval
        ) : Outcome

        data object RemovalProjected : Outcome

        data object VerificationRefreshed : Outcome
    }
}
