package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupMemberRemovalProgress
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindGroupMemberRemovalProgressUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.FindMemberDisplayNameUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.RemoveGroupMemberUseCase
import kotlin.reflect.KClass

/**
 * LoadContext -> FindRemovalProgress -> [RemoveMember] -> ResolveMemberName -> ProjectRemoval -> RefreshVerification.
 *
 * Membership commits the removal in one step; projection and verification follow. A retry after a crash or a
 * failed later step finds the committed removal and continues with the missing steps.
 */
class RemoveGroupMemberWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: GroupConversationRepository,
    private val findGroupMemberRemovalProgress: FindGroupMemberRemovalProgressUseCase,
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
            is Step.FindRemovalProgress ->
                Outcome.RemovalProgressFound(
                    findGroupMemberRemovalProgress(step.command.groupId, step.command.contactId).getOrThrow(),
                    step.context
                )
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
            is Outcome.ContextLoaded -> Transition.Next(Step.FindRemovalProgress(outcome.context, step.command))
            is Outcome.RemovalProgressFound -> continueRemoval(outcome.progress, outcome.context, step.command)
            is Outcome.MemberRemoved -> Transition.Next(Step.ResolveMemberName(outcome.removal, step.command))
            is Outcome.MemberNameResolved -> Transition.Next(Step.ProjectRemoval(outcome.removal, step.command))
            Outcome.RemovalProjected -> Transition.Next(Step.RefreshVerification(step.command))
            Outcome.VerificationRefreshed -> Transition.Complete()
        }

    private fun continueRemoval(
        progress: GroupMemberRemovalProgress,
        context: GroupMembershipContext,
        command: GroupAdministrationCommand.RemoveMember
    ): Transition<Step> =
        when (progress) {
            GroupMemberRemovalProgress.NotStarted -> Transition.Next(Step.RemoveMember(context, command))
            is GroupMemberRemovalProgress.Committed ->
                Transition.Next(Step.ResolveMemberName(progress.removal, command))
            GroupMemberRemovalProgress.Projected -> Transition.Next(Step.RefreshVerification(command))
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.RemoveMember

        data class LoadContext(
            override val command: GroupAdministrationCommand.RemoveMember
        ) : Step

        data class FindRemovalProgress(
            val context: GroupMembershipContext,
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

        data class RemovalProgressFound(
            val progress: GroupMemberRemovalProgress,
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
