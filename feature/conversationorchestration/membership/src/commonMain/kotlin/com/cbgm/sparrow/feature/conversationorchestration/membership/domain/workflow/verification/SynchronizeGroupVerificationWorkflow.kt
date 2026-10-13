package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.BroadcastGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.CheckGroupOwnershipUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RefreshOwnedGroupVerificationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RequestGroupVerificationSnapshotUseCase
import kotlin.reflect.KClass

/**
 * LoadSecurity -> RefreshRows -> BroadcastSnapshot   (local admin)
 * LoadSecurity -> RequestSnapshot                     (member)
 * LoadSecurity -> CheckOwnership -> RefreshRows       (no security state left)
 */
class SynchronizeGroupVerificationWorkflow internal constructor(
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val checkGroupOwnership: CheckGroupOwnershipUseCase,
    private val refreshOwnedGroupVerification: RefreshOwnedGroupVerificationUseCase,
    private val broadcastGroupVerificationSnapshot: BroadcastGroupVerificationSnapshotUseCase,
    private val requestGroupVerificationSnapshot: RequestGroupVerificationSnapshotUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.SynchronizeVerification,
        SynchronizeGroupVerificationWorkflow.Step,
        SynchronizeGroupVerificationWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.SynchronizeVerification> =
        GroupAdministrationCommand.SynchronizeVerification::class

    override fun lockKey(command: GroupAdministrationCommand.SynchronizeVerification): String =
        groupVerificationLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.SynchronizeVerification): Step =
        Step.LoadSecurity(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadSecurity -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                val security = verificationInputs.membershipContext(step.command.groupId).security
                when {
                    security == null -> Outcome.NoSecurityState
                    security.isLocalAdmin -> Outcome.LocalAdmin
                    else -> Outcome.Member(security.ownerContactId)
                }
            }
            is Step.CheckOwnership -> Outcome.OwnershipChecked(checkGroupOwnership(step.command.groupId).getOrThrow())
            is Step.RefreshRows -> {
                refreshOwnedGroupVerification(step.command.groupId).getOrThrow()
                Outcome.RowsRefreshed(step.broadcastAfterwards)
            }
            is Step.BroadcastSnapshot -> {
                broadcastGroupVerificationSnapshot(step.command.groupId).getOrThrow()
                Outcome.SnapshotBroadcast
            }
            is Step.RequestSnapshot -> {
                requestGroupVerificationSnapshot(step.command.groupId, step.adminContactId).getOrThrow()
                Outcome.SnapshotRequested
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NoSecurityState -> Transition.Next(Step.CheckOwnership(step.command))
            Outcome.LocalAdmin -> Transition.Next(Step.RefreshRows(broadcastAfterwards = true, command = step.command))
            is Outcome.Member ->
                outcome.adminContactId
                    ?.let { Transition.Next(Step.RequestSnapshot(it, step.command)) }
                    ?: Transition.Complete()
            is Outcome.OwnershipChecked ->
                if (outcome.ownsGroup) {
                    Transition.Next(Step.RefreshRows(broadcastAfterwards = false, command = step.command))
                } else {
                    Transition.Complete()
                }
            is Outcome.RowsRefreshed ->
                if (outcome.broadcastAfterwards) {
                    Transition.Next(
                        Step.BroadcastSnapshot(step.command)
                    )
                } else {
                    Transition.Complete()
                }
            Outcome.SnapshotBroadcast -> Transition.Complete()
            Outcome.SnapshotRequested -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.SynchronizeVerification

        data class LoadSecurity(
            override val command: GroupAdministrationCommand.SynchronizeVerification
        ) : Step

        data class CheckOwnership(
            override val command: GroupAdministrationCommand.SynchronizeVerification
        ) : Step

        data class RefreshRows(
            val broadcastAfterwards: Boolean,
            override val command: GroupAdministrationCommand.SynchronizeVerification
        ) : Step

        data class BroadcastSnapshot(
            override val command: GroupAdministrationCommand.SynchronizeVerification
        ) : Step

        data class RequestSnapshot(
            val adminContactId: String,
            override val command: GroupAdministrationCommand.SynchronizeVerification
        ) : Step
    }

    sealed interface Outcome {
        data object NoSecurityState : Outcome

        data object LocalAdmin : Outcome

        data class Member(
            val adminContactId: String?
        ) : Outcome

        data class OwnershipChecked(
            val ownsGroup: Boolean
        ) : Outcome

        data class RowsRefreshed(
            val broadcastAfterwards: Boolean
        ) : Outcome

        data object SnapshotBroadcast : Outcome

        data object SnapshotRequested : Outcome
    }
}
