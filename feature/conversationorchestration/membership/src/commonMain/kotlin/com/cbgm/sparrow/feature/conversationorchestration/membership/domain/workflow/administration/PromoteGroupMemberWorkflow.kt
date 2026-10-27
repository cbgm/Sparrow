package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.usecase.PromoteGroupMemberUseCase
import kotlin.reflect.KClass

/**
 * LoadContext -> PromoteMember -> ProjectPromotion -> RefreshVerification.
 *
 * A retry after a crash or a failed later step is safe: Membership returns the promotion of a member who is an
 * admin already without rotating again, so the projection and the verification refresh still run.
 */
class PromoteGroupMemberWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: GroupConversationRepository,
    private val promoteGroupMember: PromoteGroupMemberUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.PromoteMember,
        PromoteGroupMemberWorkflow.Step,
        PromoteGroupMemberWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.PromoteMember> =
        GroupAdministrationCommand.PromoteMember::class

    override fun lockKey(command: GroupAdministrationCommand.PromoteMember): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.PromoteMember): Step = Step.LoadContext(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadContext ->
                Outcome.ContextLoaded(conversationRepository.getMembershipContext(step.command.groupId).getOrThrow())
            is Step.PromoteMember ->
                Outcome.MemberPromoted(
                    promoteGroupMember(step.command.groupId, step.command.contactId, step.context).getOrThrow()
                )
            is Step.ProjectPromotion -> {
                conversationRepository.promoteParticipant(
                    groupId = step.promotion.groupId,
                    contactId = step.promotion.contactId,
                    updatedAtEpochMilliseconds = step.promotion.updatedAtEpochMilliseconds
                ).getOrThrow()
                Outcome.PromotionProjected
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
            is Outcome.ContextLoaded -> Transition.Next(Step.PromoteMember(outcome.context, step.command))
            is Outcome.MemberPromoted -> Transition.Next(Step.ProjectPromotion(outcome.promotion, step.command))
            Outcome.PromotionProjected -> Transition.Next(Step.RefreshVerification(step.command))
            Outcome.VerificationRefreshed -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.PromoteMember

        data class LoadContext(
            override val command: GroupAdministrationCommand.PromoteMember
        ) : Step

        data class PromoteMember(
            val context: GroupMembershipContext,
            override val command: GroupAdministrationCommand.PromoteMember
        ) : Step

        data class ProjectPromotion(
            val promotion: GroupMemberPromotionResult,
            override val command: GroupAdministrationCommand.PromoteMember
        ) : Step

        data class RefreshVerification(
            override val command: GroupAdministrationCommand.PromoteMember
        ) : Step
    }

    sealed interface Outcome {
        data class ContextLoaded(
            val context: GroupMembershipContext
        ) : Outcome

        data class MemberPromoted(
            val promotion: GroupMemberPromotionResult
        ) : Outcome

        data object PromotionProjected : Outcome

        data object VerificationRefreshed : Outcome
    }
}
