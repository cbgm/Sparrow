package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.membership.domain.usecase.ReceiveGroupMembershipReceiptUseCase
import kotlin.reflect.KClass

/**
 * ReceiveReceipt: Membership verifies the delivery receipt of a group invitation.
 *
 * The receipt never accepts the membership or authorizes a welcome, and a restored member may sign it with
 * new keys; the pinned identity therefore stays unchanged. The join request establishes authorization later.
 */
class ReceiveGroupInviteReceiptWorkflow internal constructor(
    private val receiveGroupMembershipReceipt: ReceiveGroupMembershipReceiptUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupInviteReceipt,
        ReceiveGroupInviteReceiptWorkflow.Step,
        ReceiveGroupInviteReceiptWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupInviteReceipt> =
        IncomingCommand.ReceiveGroupInviteReceipt::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupInviteReceipt): Step = Step.ReceiveReceipt(command)

    override suspend fun perform(step: Step): Outcome {
        receiveGroupMembershipReceipt(step.command.context.contactId, step.command.packet).getOrThrow()
        return Outcome.ReceiptReceived
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = Transition.Complete()

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupInviteReceipt

        data class ReceiveReceipt(
            override val command: IncomingCommand.ReceiveGroupInviteReceipt
        ) : Step
    }

    sealed interface Outcome {
        data object ReceiptReceived : Outcome
    }
}
