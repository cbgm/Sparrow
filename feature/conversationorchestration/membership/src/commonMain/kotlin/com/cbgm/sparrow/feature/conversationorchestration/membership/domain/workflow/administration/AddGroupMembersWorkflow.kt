package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RecordGroupInvitationUseCase
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.usecase.StartGroupMembershipUseCase
import kotlin.reflect.KClass

/**
 * LoadTitle -> (StartMembership -> RecordInvitation -> RefreshVerification) for every contact.
 *
 * The verification projection shows each invited member as pending right after its invitation.
 */
class AddGroupMembersWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val conversationRepository: GroupConversationRepository,
    private val startGroupMembership: StartGroupMembershipUseCase,
    private val recordGroupInvitation: RecordGroupInvitationUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.AddMembers,
        AddGroupMembersWorkflow.Step,
        AddGroupMembersWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.AddMembers> =
        GroupAdministrationCommand.AddMembers::class

    override fun lockKey(command: GroupAdministrationCommand.AddMembers): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.AddMembers): Step = Step.LoadTitle(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadTitle -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                Outcome.TitleLoaded(
                    conversationRepository.getMembershipContext(step.command.groupId).getOrThrow().title
                )
            }
            is Step.StartMembership ->
                Outcome.MembershipStarted(
                    started = startGroupMembership(step.command.groupId, step.title, step.contactId).getOrThrow(),
                    title = step.title,
                    remaining = step.remaining
                )
            is Step.RecordInvitation -> {
                recordGroupInvitation(step.started, step.title).getOrThrow()
                Outcome.InvitationRecorded(step.title, step.remaining)
            }
            is Step.RefreshVerification -> {
                engine.run(
                    MembershipCommand.RefreshOwnedGroupVerification(step.command.groupId)
                ).getOrThrow()
                Outcome.VerificationRefreshed(step.title, step.remaining)
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.TitleLoaded -> nextMember(step.command.contactIds.toList(), outcome.title, step.command)
            is Outcome.MembershipStarted ->
                Transition.Next(Step.RecordInvitation(outcome.started, outcome.title, outcome.remaining, step.command))
            is Outcome.InvitationRecorded ->
                Transition.Next(Step.RefreshVerification(outcome.title, outcome.remaining, step.command))
            is Outcome.VerificationRefreshed -> nextMember(outcome.remaining, outcome.title, step.command)
        }

    private fun nextMember(
        remaining: List<String>,
        title: String,
        command: GroupAdministrationCommand.AddMembers
    ): Transition<Step> =
        remaining.firstOrNull()
            ?.let { contactId -> Transition.Next(Step.StartMembership(contactId, title, remaining.drop(1), command)) }
            ?: Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.AddMembers

        data class LoadTitle(
            override val command: GroupAdministrationCommand.AddMembers
        ) : Step

        /** [remaining] are the contacts still to invite after this one. */
        data class StartMembership(
            val contactId: String,
            val title: String,
            val remaining: List<String>,
            override val command: GroupAdministrationCommand.AddMembers
        ) : Step

        data class RecordInvitation(
            val started: StartedMembershipHandshake,
            val title: String,
            val remaining: List<String>,
            override val command: GroupAdministrationCommand.AddMembers
        ) : Step

        data class RefreshVerification(
            val title: String,
            val remaining: List<String>,
            override val command: GroupAdministrationCommand.AddMembers
        ) : Step
    }

    sealed interface Outcome {
        data class TitleLoaded(
            val title: String
        ) : Outcome

        data class MembershipStarted(
            val started: StartedMembershipHandshake,
            val title: String,
            val remaining: List<String>
        ) : Outcome

        data class InvitationRecorded(
            val title: String,
            val remaining: List<String>
        ) : Outcome

        data class VerificationRefreshed(
            val title: String,
            val remaining: List<String>
        ) : Outcome
    }
}
