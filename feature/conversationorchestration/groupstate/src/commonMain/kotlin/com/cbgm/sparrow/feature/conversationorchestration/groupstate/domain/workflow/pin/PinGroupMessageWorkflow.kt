package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.pin

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinPreparation
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindPinnableGroupMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.PrepareGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.ResolveGroupPinSenderKeyUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.SaveGroupPinUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import kotlin.reflect.KClass

/**
 * FindPinTarget -> [ResolveSenderKey] -> AuthorizeAdmin -> PreparePin -> SavePin -> PublishPin.
 *
 * The pin snapshot keeps the sender's signing key so every member can verify who wrote the message. The pin is
 * saved as unpublished right away, so the user sees it at once; only the publishing
 * ([GroupStateCommand.PublishGroupPin]) waits for the group administration lock. A pin that was never published is
 * not skipped as "already pinned": pinning it again, or the recovery after the next start, publishes it.
 */
class PinGroupMessageWorkflow internal constructor(
    private val engine: WorkflowEngine,
    private val pinRepository: GroupPinRepository,
    private val findPinnableGroupMessage: FindPinnableGroupMessageUseCase,
    private val resolveGroupPinSenderKey: ResolveGroupPinSenderKeyUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val prepareGroupPin: PrepareGroupPinUseCase,
    private val saveGroupPin: SaveGroupPinUseCase
) : ConversationWorkflow<
        GroupMessageCommand.PinMessage,
        PinGroupMessageWorkflow.Step,
        PinGroupMessageWorkflow.Outcome
    > {
    override val commandType: KClass<GroupMessageCommand.PinMessage> = GroupMessageCommand.PinMessage::class

    override fun firstStep(command: GroupMessageCommand.PinMessage): Step = Step.FindPinTarget(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindPinTarget -> findPinTarget(step.command)
            is Step.ResolveSenderKey ->
                Outcome.SenderKeyResolved(
                    step.message,
                    resolveGroupPinSenderKey(step.command.groupId, step.message).getOrThrow()
                )
            is Step.AuthorizeAdmin -> {
                requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                Outcome.AdminAuthorized(message = step.message, senderKey = step.senderKey)
            }
            is Step.PreparePin ->
                pinPrepared(prepareGroupPin(step.command.groupId, step.message, step.senderKey).getOrThrow())
            is Step.SavePin -> {
                saveGroupPin(step.pin).getOrThrow()
                Outcome.PinSaved
            }
            is Step.PublishPin -> {
                engine.run(GroupStateCommand.PublishGroupPin(step.command.groupId)).getOrThrow()
                Outcome.PinPublished
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
            is Outcome.PinPrepared -> Transition.Next(Step.SavePin(outcome.pin, step.command))
            Outcome.PinSaved -> Transition.Next(Step.PublishPin(step.command))
            Outcome.PinPublished -> Transition.Complete()
        }

    private suspend fun findPinTarget(command: GroupMessageCommand.PinMessage): Outcome {
        require(command.groupId.isNotBlank()) { "Group ID must not be blank" }
        val message = findPinnableGroupMessage(command.groupId, command.messageId).getOrThrow()
        val current = pinRepository.find(command.groupId).getOrThrow()
        return if (current?.messageId == message.id && current?.publishPending == false) {
            Outcome.TargetAlreadyPinned
        } else {
            Outcome.TargetFound(message)
        }
    }

    /** A message that was pinned (and published) already only needed the admin check. */
    private fun afterAdminAuthorized(
        outcome: Outcome.AdminAuthorized,
        command: GroupMessageCommand.PinMessage
    ): Transition<Step> =
        if (outcome.message == null || outcome.senderKey == null) {
            Transition.Complete()
        } else {
            Transition.Next(Step.PreparePin(outcome.message, outcome.senderKey, command))
        }

    private fun pinPrepared(result: GroupPinPreparation): Outcome =
        when (result) {
            is GroupPinPreparation.Ready -> Outcome.PinPrepared(result.pin)
            GroupPinPreparation.AlreadyPinned -> Outcome.AlreadyPinned
            GroupPinPreparation.SlotHeldByExpenses -> Outcome.SlotOccupiedByExpenses
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

        data class PreparePin(
            val message: StoredMessage,
            val senderKey: ByteArray,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class SavePin(
            val pin: GroupPinState,
            override val command: GroupMessageCommand.PinMessage
        ) : Step

        data class PublishPin(
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
            val message: StoredMessage?,
            val senderKey: ByteArray?
        ) : Outcome

        data object AlreadyPinned : Outcome

        data object SlotOccupiedByExpenses : Outcome

        data class PinPrepared(
            val pin: GroupPinState
        ) : Outcome

        data object PinSaved : Outcome

        data object PinPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "pinned message"
    }
}
