package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.BroadcastGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ConfirmParticipantVerificationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.SendAdminVerificationReceiptUseCase
import kotlin.reflect.KClass

/**
 * LoadSecurity -> ConfirmParticipant -> BroadcastSnapshot   (the admin verified a member)
 * LoadSecurity -> SendReceipt                               (a member verified the admin)
 */
class VerifyGroupMemberWorkflow internal constructor(
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val confirmParticipantVerification: ConfirmParticipantVerificationUseCase,
    private val broadcastGroupVerificationSnapshot: BroadcastGroupVerificationSnapshotUseCase,
    private val sendAdminVerificationReceipt: SendAdminVerificationReceiptUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.VerifyMember,
        VerifyGroupMemberWorkflow.Step,
        VerifyGroupMemberWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.VerifyMember> =
        GroupAdministrationCommand.VerifyMember::class

    override fun lockKey(command: GroupAdministrationCommand.VerifyMember): String =
        groupVerificationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.VerifyMember): Step = Step.LoadSecurity(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadSecurity -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                require(step.command.contactId.isNotBlank()) { "Contact ID must not be blank" }
                val security =
                    verificationInputs.membershipContext(step.command.groupId).getOrThrow().security
                        ?: error("Group security state was not found")
                when {
                    security.isLocalAdmin -> Outcome.LocalAdmin
                    security.ownerContactId == step.command.contactId -> Outcome.TargetIsAdmin
                    else -> Outcome.TargetIsNotAdmin
                }
            }
            is Step.ConfirmParticipant -> {
                confirmParticipantVerification(step.command.groupId, step.command.contactId).getOrThrow()
                Outcome.ParticipantConfirmed
            }
            is Step.BroadcastSnapshot -> {
                broadcastGroupVerificationSnapshot(step.command.groupId).getOrThrow()
                Outcome.SnapshotBroadcast
            }
            is Step.SendReceipt -> {
                sendAdminVerificationReceipt(step.command.groupId, step.command.contactId).getOrThrow()
                Outcome.ReceiptSent
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.LocalAdmin -> Transition.Next(Step.ConfirmParticipant(step.command))
            Outcome.TargetIsAdmin -> Transition.Next(Step.SendReceipt(step.command))
            Outcome.TargetIsNotAdmin ->
                Transition.Reject(
                    ConversationFailure(
                        ConversationFailureReason.NOT_ALLOWED,
                        "A group participant may verify only the group admin"
                    )
                )
            Outcome.ParticipantConfirmed -> Transition.Next(Step.BroadcastSnapshot(step.command))
            Outcome.SnapshotBroadcast -> Transition.Complete()
            Outcome.ReceiptSent -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.VerifyMember

        data class LoadSecurity(
            override val command: GroupAdministrationCommand.VerifyMember
        ) : Step

        data class ConfirmParticipant(
            override val command: GroupAdministrationCommand.VerifyMember
        ) : Step

        data class BroadcastSnapshot(
            override val command: GroupAdministrationCommand.VerifyMember
        ) : Step

        data class SendReceipt(
            override val command: GroupAdministrationCommand.VerifyMember
        ) : Step
    }

    sealed interface Outcome {
        data object LocalAdmin : Outcome

        data object TargetIsAdmin : Outcome

        data object TargetIsNotAdmin : Outcome

        data object ParticipantConfirmed : Outcome

        data object SnapshotBroadcast : Outcome

        data object ReceiptSent : Outcome
    }
}
