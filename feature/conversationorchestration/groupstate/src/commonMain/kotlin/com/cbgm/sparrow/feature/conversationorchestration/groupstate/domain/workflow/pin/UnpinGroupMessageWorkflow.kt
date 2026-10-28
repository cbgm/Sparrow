package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSlotPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareEmptyGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import kotlin.reflect.KClass

/**
 * Removes a pinned message; an open expense board must be closed instead.
 *
 * AuthorizeAdmin -> FindPin -> [PrepareEmptyPin -> SaveEmptyPin] -> PublishPin.
 *
 * The slot is cleared locally right away as unpublished, so the user sees it at once; only the publishing
 * ([GroupStateCommand.PublishGroupPin]) waits for the group administration lock. An empty slot that was never
 * published is published again instead of reported as "nothing pinned".
 */
class UnpinGroupMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val prepareEmptyGroupPin: PrepareEmptyGroupPinUseCase,
    private val saveGroupPin: SaveGroupPinUseCase
) : ConversationWorkflow<
        GroupMessageCommand.UnpinMessage,
        UnpinGroupMessageWorkflow.Step,
        UnpinGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.UnpinMessage> = GroupMessageCommand.UnpinMessage::class

    override fun firstStep(command: GroupMessageCommand.UnpinMessage): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                Outcome.AdminAuthorized
            }
            is Step.FindPin -> pinFound(pinRepository.find(step.command.groupId).getOrThrow())
            is Step.PrepareEmptyPin ->
                Outcome.EmptyPinPrepared(prepareEmptyGroupPin.unpublished(step.command.groupId, step.pin).getOrThrow())
            is Step.SaveEmptyPin -> {
                saveGroupPin(step.pin).getOrThrow()
                Outcome.EmptyPinSaved
            }
            is Step.PublishPin -> {
                engine.run(GroupStateCommand.PublishGroupPin(step.command.groupId)).getOrThrow()
                Outcome.PinPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.AdminAuthorized -> Transition.Next(Step.FindPin(step.command))
            Outcome.NothingPinned -> Transition.Complete()
            Outcome.UnpinNotPublished -> Transition.Next(Step.PublishPin(step.command))
            Outcome.PinNotRemovable ->
                Transition.Reject(
                    ConversationFailure(
                        ConversationFailureReason.NOT_ALLOWED,
                        "Close group expenses before removing the expense board pin"
                    )
                )
            is Outcome.PinFound -> Transition.Next(Step.PrepareEmptyPin(outcome.pin, step.command))
            is Outcome.EmptyPinPrepared -> Transition.Next(Step.SaveEmptyPin(outcome.pin, step.command))
            Outcome.EmptyPinSaved -> Transition.Next(Step.PublishPin(step.command))
            Outcome.PinPublished -> Transition.Complete()
        }

    /**
     * An open expense board holds the slot until the expenses are closed. An empty slot that was saved but never
     * published still has to reach the members.
     */
    private fun pinFound(pin: GroupPinState?): Outcome =
        when {
            pin?.messageId == null && pin?.publishPending == true -> Outcome.UnpinNotPublished
            pin?.messageId == null -> Outcome.NothingPinned
            GroupPinSlotPolicy.isHeldByExpenses(pin) -> Outcome.PinNotRemovable
            else -> Outcome.PinFound(pin)
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.UnpinMessage

        data class AuthorizeAdmin(
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class FindPin(
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class PrepareEmptyPin(
            val pin: GroupPinState,
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class SaveEmptyPin(
            val pin: GroupPinState,
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class PublishPin(
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step
    }

    sealed interface Outcome {
        data object AdminAuthorized : Outcome

        data object NothingPinned : Outcome

        data object UnpinNotPublished : Outcome

        data object PinNotRemovable : Outcome

        data class PinFound(
            val pin: GroupPinState
        ) : Outcome

        data class EmptyPinPrepared(
            val pin: GroupPinState
        ) : Outcome

        data object EmptyPinSaved : Outcome

        data object PinPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
