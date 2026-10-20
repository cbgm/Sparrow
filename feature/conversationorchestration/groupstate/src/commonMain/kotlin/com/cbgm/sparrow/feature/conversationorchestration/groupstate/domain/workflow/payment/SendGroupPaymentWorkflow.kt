package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.payment

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupPaymentTransferUseCase
import kotlin.reflect.KClass

/** ValidatePayment -> SendPayment; bank details never enter messaging or the outbox. */
class SendGroupPaymentWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val requirePaymentTransfer: RequireGroupPaymentTransferUseCase
) : ConversationWorkflow<GroupMessageCommand.SendPaymentTransfer, SendGroupPaymentWorkflow.Step, SendGroupPaymentWorkflow.Outcome> {
    override val commandType: KClass<GroupMessageCommand.SendPaymentTransfer> = GroupMessageCommand.SendPaymentTransfer::class

    override fun lockKey(command: GroupMessageCommand.SendPaymentTransfer): String = "payment:${command.groupId}"

    override fun firstStep(command: GroupMessageCommand.SendPaymentTransfer): Step = Step.ValidatePayment(command)

    override suspend fun perform(step: Step): Outcome = when (step) {
        is Step.ValidatePayment -> {
            requirePaymentTransfer(step.command.groupId, step.command.event).getOrThrow()
            Outcome.Validated
        }
        is Step.SendPayment -> Outcome.Sent(
            engine.run(GroupMessageCommand.SendMessage(step.command.groupId, listOf(step.command.event)))
                .getOrThrow() as ConversationResult.GroupMessageSent
        )
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = when (outcome) {
        Outcome.Validated -> Transition.Next(Step.SendPayment(step.command))
        is Outcome.Sent -> Transition.Complete(outcome.result)
    }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.SendPaymentTransfer

        data class ValidatePayment(
            override val command: GroupMessageCommand.SendPaymentTransfer
        ) : Step

        data class SendPayment(
            override val command: GroupMessageCommand.SendPaymentTransfer
        ) : Step
    }

    sealed interface Outcome {
        data object Validated : Outcome

        data class Sent(
            val result: ConversationResult.GroupMessageSent
        ) : Outcome
    }
}
