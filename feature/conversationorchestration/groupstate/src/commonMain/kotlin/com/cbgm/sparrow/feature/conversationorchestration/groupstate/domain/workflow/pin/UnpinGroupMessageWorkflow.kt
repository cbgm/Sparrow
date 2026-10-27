package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSlotPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.ClearGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/**
 * Removes a pinned message; an open expense board must be closed instead.
 *
 * AuthorizeAdmin -> FindPin -> SaveEmptyPin -> CreatePinPackets -> EnqueuePinPackets.
 */
class UnpinGroupMessageWorkflow internal constructor(
    private val pinRepository: GroupPinRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val clearGroupPin: ClearGroupPinUseCase,
    private val createGroupPinPackets: CreateGroupPinPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.UnpinMessage,
        UnpinGroupMessageWorkflow.Step,
        UnpinGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.UnpinMessage> = GroupMessageCommand.UnpinMessage::class

    override fun lockKey(command: GroupMessageCommand.UnpinMessage): String = groupPinLockKey(command.groupId)

    override fun firstStep(command: GroupMessageCommand.UnpinMessage): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            is Step.FindPin -> pinFound(pinRepository.find(step.command.groupId).getOrThrow(), step.context)
            is Step.SaveEmptyPin ->
                Outcome.EmptyPinSaved(clearGroupPin(step.command.groupId, step.pin).getOrThrow(), step.context)
            is Step.CreatePinPackets ->
                Outcome.PinPacketsCreated(createGroupPinPackets(step.pin, step.context).getOrThrow())
            is Step.EnqueuePinPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.PinPacketsEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.AdminAuthorized -> Transition.Next(Step.FindPin(outcome.context, step.command))
            Outcome.NothingPinned -> Transition.Complete()
            Outcome.PinNotRemovable ->
                Transition.Reject(
                    ConversationFailure(
                        ConversationFailureReason.NOT_ALLOWED,
                        "Close group expenses before removing the expense board pin"
                    )
                )
            is Outcome.PinFound -> Transition.Next(Step.SaveEmptyPin(outcome.pin, outcome.context, step.command))
            is Outcome.EmptyPinSaved ->
                Transition.Next(Step.CreatePinPackets(outcome.pin, outcome.context, step.command))
            is Outcome.PinPacketsCreated -> Transition.Next(Step.EnqueuePinPackets(outcome.packets, step.command))
            Outcome.PinPacketsEnqueued -> Transition.Complete()
        }

    /** An open expense board holds the slot until the expenses are closed. */
    private fun pinFound(pin: GroupPinState?, context: GroupAdminContext): Outcome =
        when {
            pin?.messageId == null -> Outcome.NothingPinned
            GroupPinSlotPolicy.isHeldByExpenses(pin) -> Outcome.PinNotRemovable
            else -> Outcome.PinFound(pin, context)
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.UnpinMessage

        data class AuthorizeAdmin(
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class FindPin(
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class SaveEmptyPin(
            val pin: GroupPinState,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class CreatePinPackets(
            val pin: GroupPinState,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step

        data class EnqueuePinPackets(
            val packets: Map<String, GroupPinUpdatedPacket>,
            override val command: GroupMessageCommand.UnpinMessage
        ) : Step
    }

    sealed interface Outcome {
        data class AdminAuthorized(
            val context: GroupAdminContext
        ) : Outcome

        data object NothingPinned : Outcome

        data object PinNotRemovable : Outcome

        data class PinFound(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class EmptyPinSaved(
            val pin: GroupPinState,
            val context: GroupAdminContext
        ) : Outcome

        data class PinPacketsCreated(
            val packets: Map<String, GroupPinUpdatedPacket>
        ) : Outcome

        data object PinPacketsEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
