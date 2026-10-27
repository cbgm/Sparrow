package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSaveResult
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindPinnableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.ResolveGroupPinSenderKeyUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/**
 * FindPinTarget -> [ResolveSenderKey] -> AuthorizeAdmin -> SavePin -> CreatePinPackets -> EnqueuePinPackets.
 *
 * The pin snapshot keeps the sender's signing key so every member can verify who wrote the message.
 */
class PinGroupMessageWorkflow internal constructor(
    private val pinRepository: GroupPinRepository,
    private val findPinnableGroupMessage: FindPinnableGroupMessageUseCase,
    private val resolveGroupPinSenderKey: ResolveGroupPinSenderKeyUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val saveGroupPin: SaveGroupPinUseCase,
    private val createGroupPinPackets: CreateGroupPinPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupMessageCommand.PinMessage,
        PinGroupMessageWorkflow.Step,
        PinGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.PinMessage> = GroupMessageCommand.PinMessage::class

    override fun lockKey(command: GroupMessageCommand.PinMessage): String = groupPinLockKey(command.groupId)

    override fun firstStep(command: GroupMessageCommand.PinMessage): Step = Step.FindPinTarget(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindPinTarget -> findPinTarget(step.command)
            is Step.ResolveSenderKey ->
                Outcome.SenderKeyResolved(
                    step.message,
                    resolveGroupPinSenderKey(step.command.groupId, step.message).getOrThrow()
                )
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(
                    context = requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow(),
                    message = step.message,
                    senderKey = step.senderKey
                )
            is Step.SavePin ->
                pinSaved(
                    saveGroupPin(step.command.groupId, step.message, step.senderKey).getOrThrow(),
                    step.context
                )
            is Step.CreatePinPackets ->
                Outcome.PinPacketsCreated(createGroupPinPackets(step.pin, step.context).getOrThrow())
            is Step.EnqueuePinPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.PinPacketsEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.TargetAlreadyPinned ->
                Transition.Next(Step.AuthorizeAdmin(message = null, senderKey = null, command = step.command))
            is Outcome.TargetFound -> Transition.Next(Step.ResolveSenderKey(outcome.message, step.command))
            is Outcome.SenderKeyResolved ->
                Transition.Next(Step.AuthorizeAdmin(outcome.message, outcome.senderKey, step.command))
            is Outcome.AdminAuthorized -> afterAdminAuthorized(outcome, step.command)
            Outcome.AlreadyPinned -> Transition.Complete()
            Outcome.SlotOccupiedByExpenses ->
                Transition.Reject(
                    ConversationFailure(
                        ConversationFailureReason.NOT_ALLOWED,
                        "An active expense board occupies the group pin"
                    )
                )
            is Outcome.PinSaved -> Transition.Next(Step.CreatePinPackets(outcome.pin, outcome.context, step.command))
            is Outcome.PinPacketsCreated -> Transition.Next(Step.EnqueuePinPackets(outcome.packets, step.command))
            Outcome.PinPacketsEnqueued -> Transition.Complete()
        }

    private suspend fun findPinTarget(command: GroupMessageCommand.PinMessage): Outcome {
        require(command.groupId.isNotBlank()) { "Group ID must not be blank" }
        val message = findPinnableGroupMessage(command.groupId, command.messageId).getOrThrow()
        return if (pinRepository.find(command.groupId).getOrThrow()?.messageId == message.id) {
            Outcome.TargetAlreadyPinned
        } else {
            Outcome.TargetFound(message)
        }
    }

    /** A message that was pinned already only needed the admin check. */
    private fun afterAdminAuthorized(
        outcome: Outcome.AdminAuthorized,
        command: GroupMessageCommand.PinMessage
    ): Transition<Step> =
        if (outcome.message == null || outcome.senderKey == null) {
            Transition.Complete()
        } else {
            Transition.Next(Step.SavePin(outcome.message, outcome.senderKey, outcome.context, command))
        }

    private fun pinSaved(result: GroupPinSaveResult, context: GroupAdminContext): Outcome =
        when (result) {
            is GroupPinSaveResult.Saved -> Outcome.PinSaved(result.pin, context)
            GroupPinSaveResult.AlreadyPinned -> Outcome.AlreadyPinned
            GroupPinSaveResult.SlotHeldByExpenses -> Outcome.SlotOccupiedByExpenses
        }

    sealed interface Step : WorkflowStep {
        val command: GroupMessageCommand.PinMessage

        data class FindPinTarget(
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class ResolveSenderKey(
            val message: StoredMessage,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        /** [message] is null when it is pinned already; the admin check still applies. */
        data class AuthorizeAdmin(
            val message: StoredMessage?,
            val senderKey: ByteArray?,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class SavePin(
            val message: StoredMessage,
            val senderKey: ByteArray,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class CreatePinPackets(
            val pin: GroupPinState,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class EnqueuePinPackets(
            val packets: Map<String, GroupPinUpdatedPacket>,
            override val command: GroupMessageCommand.PinMessage
        ) : Step
    }

    sealed interface Outcome {
        data object TargetAlreadyPinned : Outcome

        data class TargetFound(
            val message: StoredMessage
        ) : Outcome

        data class SenderKeyResolved(
            val message: StoredMessage,
            val senderKey: ByteArray
        ) : Outcome

        data class AdminAuthorized(
            val context: GroupAdminContext,
            val message: StoredMessage?,
            val senderKey: ByteArray?
        ) : Outcome

        data object AlreadyPinned : Outcome

        data object SlotOccupiedByExpenses : Outcome

        data class PinSaved(
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
