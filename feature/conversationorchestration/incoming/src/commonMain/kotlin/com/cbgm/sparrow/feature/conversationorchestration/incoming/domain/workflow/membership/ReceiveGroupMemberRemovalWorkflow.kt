package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.membership

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.command.IncomingCommand
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ApplyIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.AuthorizeIncomingGroupRemovalUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.CompleteIncomingGroupRemovalUseCase
import kotlin.reflect.KClass

/** AuthorizeRemoval -> ApplyRemoval -> CompleteRemoval; Membership resolves the trusted signing key. */
class ReceiveGroupMemberRemovalWorkflow internal constructor(
    private val authorizeIncomingGroupRemoval: AuthorizeIncomingGroupRemovalUseCase,
    private val applyIncomingGroupRemoval: ApplyIncomingGroupRemovalUseCase,
    private val completeIncomingGroupRemoval: CompleteIncomingGroupRemovalUseCase
) : WorkflowStateMachine<
        IncomingCommand.ReceiveGroupMemberRemoval,
        ReceiveGroupMemberRemovalWorkflow.Step,
        ReceiveGroupMemberRemovalWorkflow.Outcome
    > {
    override val commandType: KClass<IncomingCommand.ReceiveGroupMemberRemoval> =
        IncomingCommand.ReceiveGroupMemberRemoval::class

    override fun firstStep(command: IncomingCommand.ReceiveGroupMemberRemoval): Step = Step.AuthorizeRemoval(command)

    override suspend fun perform(step: Step): Outcome = when (step) {
        is Step.AuthorizeRemoval -> {
            val authorized = authorizeIncomingGroupRemoval(step.command.packet, step.command.context.contactId).getOrThrow()
            if (authorized) Outcome.RemovalAuthorized else Outcome.NotApplicable
        }
        is Step.ApplyRemoval -> {
            applyIncomingGroupRemoval(step.command.packet).getOrThrow()
            Outcome.RemovalApplied
        }
        is Step.CompleteRemoval -> {
            completeIncomingGroupRemoval(step.command.packet).getOrThrow()
            Outcome.Completed
        }
    }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> = when (outcome) {
        Outcome.RemovalAuthorized -> Transition.Next(Step.ApplyRemoval(step.command))
        Outcome.RemovalApplied -> Transition.Next(Step.CompleteRemoval(step.command))
        Outcome.NotApplicable, Outcome.Completed -> Transition.Complete()
    }

    sealed interface Step : WorkflowStep {
        val command: IncomingCommand.ReceiveGroupMemberRemoval

        data class AuthorizeRemoval(
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step

        data class ApplyRemoval(
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step

        data class CompleteRemoval(
            override val command: IncomingCommand.ReceiveGroupMemberRemoval
        ) : Step
    }

    sealed interface Outcome {
        data object RemovalAuthorized : Outcome

        data object NotApplicable : Outcome

        data object RemovalApplied : Outcome

        data object Completed : Outcome
    }
}
