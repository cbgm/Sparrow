package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.verification

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.BroadcastGroupVerificationSnapshotUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.CheckGroupOwnershipUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RefreshOwnedGroupVerificationUseCase
import kotlin.reflect.KClass

/**
 * LoadSecurity -> RefreshRows -> BroadcastSnapshot   (local admin)
 * LoadSecurity -> CheckOwnership -> RefreshRows      (owner without security state, e.g. before the first member joined)
 */
class RefreshOwnedGroupVerificationWorkflow internal constructor(
    private val verificationInputs: GroupVerificationInputsUseCase,
    private val checkGroupOwnership: CheckGroupOwnershipUseCase,
    private val refreshOwnedGroupVerification: RefreshOwnedGroupVerificationUseCase,
    private val broadcastGroupVerificationSnapshot: BroadcastGroupVerificationSnapshotUseCase
) : WorkflowStateMachine<
        MembershipCommand.RefreshOwnedGroupVerification,
        RefreshOwnedGroupVerificationWorkflow.Step,
        RefreshOwnedGroupVerificationWorkflow.Outcome
    > {
    override val commandType: KClass<MembershipCommand.RefreshOwnedGroupVerification> =
        MembershipCommand.RefreshOwnedGroupVerification::class

    override fun lockKey(command: MembershipCommand.RefreshOwnedGroupVerification): String =
        groupVerificationLockKey(command.groupId)

    override fun firstStep(command: MembershipCommand.RefreshOwnedGroupVerification): Step = Step.LoadSecurity(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.LoadSecurity -> {
                val security =
                    verificationInputs.membershipContext(step.command.groupId).getOrThrow().security
                when {
                    security == null -> Outcome.NoSecurityState
                    security.isLocalAdmin -> Outcome.LocalAdmin
                    else -> error("Only a group admin may publish membership verification state")
                }
            }
            is Step.CheckOwnership -> {
                check(checkGroupOwnership(step.command.groupId).getOrThrow()) {
                    "Only the group owner may update membership verification state"
                }
                Outcome.OwnershipConfirmed
            }
            is Step.RefreshRows -> {
                refreshOwnedGroupVerification(step.command.groupId).getOrThrow()
                Outcome.RowsRefreshed(step.broadcastAfterwards)
            }
            is Step.BroadcastSnapshot -> {
                broadcastGroupVerificationSnapshot(step.command.groupId).getOrThrow()
                Outcome.SnapshotBroadcast
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NoSecurityState -> Transition.Next(Step.CheckOwnership(step.command))
            Outcome.LocalAdmin -> Transition.Next(Step.RefreshRows(broadcastAfterwards = true, command = step.command))
            Outcome.OwnershipConfirmed ->
                Transition.Next(Step.RefreshRows(broadcastAfterwards = false, command = step.command))
            is Outcome.RowsRefreshed ->
                if (outcome.broadcastAfterwards) {
                    Transition.Next(
                        Step.BroadcastSnapshot(step.command)
                    )
                } else {
                    Transition.Complete()
                }
            Outcome.SnapshotBroadcast -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: MembershipCommand.RefreshOwnedGroupVerification

        data class LoadSecurity(
            override val command: MembershipCommand.RefreshOwnedGroupVerification
        ) : Step

        data class CheckOwnership(
            override val command: MembershipCommand.RefreshOwnedGroupVerification
        ) : Step

        data class RefreshRows(
            val broadcastAfterwards: Boolean,
            override val command: MembershipCommand.RefreshOwnedGroupVerification
        ) : Step

        data class BroadcastSnapshot(
            override val command: MembershipCommand.RefreshOwnedGroupVerification
        ) : Step
    }

    sealed interface Outcome {
        data object NoSecurityState : Outcome

        data object LocalAdmin : Outcome

        data object OwnershipConfirmed : Outcome

        data class RowsRefreshed(
            val broadcastAfterwards: Boolean
        ) : Outcome

        data object SnapshotBroadcast : Outcome
    }
}
