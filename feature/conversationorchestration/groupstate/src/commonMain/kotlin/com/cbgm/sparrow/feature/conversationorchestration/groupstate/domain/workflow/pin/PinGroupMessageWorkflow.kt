package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupPinPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindGroupMessageUseCase
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupPinSenderSigningKeyUseCase
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket
import kotlin.reflect.KClass

/**
 * FindPinTarget -> [ResolveSenderKey] -> AuthorizeAdmin -> CheckPinSlot -> SavePin -> CreatePinPackets
 * -> EnqueuePinPackets.
 *
 * The pin snapshot keeps the sender's signing key so every member can verify who wrote the message.
 */
class PinGroupMessageWorkflow internal constructor(
    private val messageRepository: GroupMessageRepository,
    private val pinRepository: GroupPinRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val findGroupMessage: FindGroupMessageUseCase,
    private val getGroupPinSenderSigningKey: GetGroupPinSenderSigningKeyUseCase,
    private val getRemoteIdentity: GetRemoteIdentityUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
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
            is Step.ResolveSenderKey -> Outcome.SenderKeyResolved(step.message, resolveSenderKey(step))
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(
                    context = requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow(),
                    message = step.message,
                    senderKey = step.senderKey
                )
            is Step.CheckPinSlot -> checkPinSlot(step)
            is Step.SavePin -> savePin(step)
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
            is Outcome.AdminAuthorized ->
                if (outcome.message == null || outcome.senderKey == null) {
                    Transition.Complete()
                } else {
                    Transition.Next(
                        Step.CheckPinSlot(outcome.message, outcome.senderKey, outcome.context, step.command)
                    )
                }
            Outcome.AlreadyPinned -> Transition.Complete()
            Outcome.SlotOccupiedByExpenses ->
                Transition.Reject(
                    ConversationFailure(
                        ConversationFailureReason.NOT_ALLOWED,
                        "An active expense board occupies the group pin"
                    )
                )
            is Outcome.SlotFree ->
                Transition.Next(
                    Step.SavePin(
                        outcome.message,
                        outcome.senderKey,
                        outcome.context,
                        outcome.previousChangedAt,
                        step.command
                    )
                )
            is Outcome.PinSaved -> Transition.Next(Step.CreatePinPackets(outcome.pin, outcome.context, step.command))
            is Outcome.PinPacketsCreated -> Transition.Next(Step.EnqueuePinPackets(outcome.packets, step.command))
            Outcome.PinPacketsEnqueued -> Transition.Complete()
        }

    private suspend fun findPinTarget(command: GroupMessageCommand.PinMessage): Outcome {
        require(command.groupId.isNotBlank()) { "Group ID must not be blank" }
        val message = findPinnableMessage(command)
        return if (pinRepository.find(command.groupId).getOrThrow()?.messageId == message.id) {
            Outcome.TargetAlreadyPinned
        } else {
            Outcome.TargetFound(message)
        }
    }

    private suspend fun findPinnableMessage(command: GroupMessageCommand.PinMessage): StoredMessage {
        val message = findGroupMessage(command.groupId, command.messageId).getOrThrow()
        check(message.isGroupUserMessage) { "Only group user messages can be pinned" }
        check(message.contentStatus == MessageContentStatus.READABLE) { "Only readable messages can be pinned" }
        return message
    }

    /** Membership knows the epoch key of current members; an earlier sender falls back to their pinned identity. */
    private suspend fun resolveSenderKey(step: Step.ResolveSenderKey): ByteArray {
        val memberKey =
            getGroupPinSenderSigningKey(
                groupId = step.command.groupId,
                isMine = step.message.isMine,
                senderContactId = step.message.senderContactId
            ).getOrThrow()
        val senderKey = memberKey ?: run {
            val contactId =
                requireNotNull(step.message.senderContactId) { "Incoming group message has no sender contact" }
            getRemoteIdentity(contactId).getOrThrow()
                ?.signingPublicKey
                ?.copyOf()
                ?: error("Pinned message sender identity was not found")
        }
        require(senderKey.isNotEmpty()) { "Pinned message sender identity was not found" }
        return senderKey
    }

    private suspend fun checkPinSlot(step: Step.CheckPinSlot): Outcome {
        val current = findPinnableMessage(step.command)
        check(current.isMine == step.message.isMine && current.senderContactId == step.message.senderContactId) {
            "Pinned message sender changed while resolving the signing identity"
        }
        val pin = pinRepository.find(step.command.groupId).getOrThrow()
        return when {
            pin?.messageId == step.message.id -> Outcome.AlreadyPinned
            pin?.hasActiveExpenseBoard == true -> Outcome.SlotOccupiedByExpenses
            else -> Outcome.SlotFree(current, step.senderKey, step.context, pin?.changedAtEpochMilliseconds ?: 0L)
        }
    }

    private suspend fun savePin(step: Step.SavePin): Outcome {
        val message = step.message
        val text = messageRepository.findText(message.id).getOrThrow()?.takeIf(String::isNotBlank)
        val changedAt = maxOf(SystemClock.nowEpochMilliseconds(), step.previousChangedAt + 1L)
        val pin =
            GroupPinState(
                groupId = step.command.groupId,
                messageId = message.id,
                messageSentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                messageSenderSigningPublicKey = step.senderKey.copyOf(),
                senderContactId = message.senderContactId,
                isMine = message.isMine,
                parts = listOfNotNull(text?.let { Text(id = message.id, text = it) }) +
                    attachmentRepository.messageParts(message.id).getOrThrow(),
                replyToMessageId = message.replyToMessageId,
                pinnedAtEpochMilliseconds = changedAt,
                changedAtEpochMilliseconds = changedAt
            )
        pinRepository.save(pin).getOrThrow()
        return Outcome.PinSaved(pin, step.context)
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

        data class CheckPinSlot(
            val message: StoredMessage,
            val senderKey: ByteArray,
            val context: GroupAdminContext,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class SavePin(
            val message: StoredMessage,
            val senderKey: ByteArray,
            val context: GroupAdminContext,
            val previousChangedAt: Long,
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

        data class SlotFree(
            val message: StoredMessage,
            val senderKey: ByteArray,
            val context: GroupAdminContext,
            val previousChangedAt: Long
        ) : Outcome

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
