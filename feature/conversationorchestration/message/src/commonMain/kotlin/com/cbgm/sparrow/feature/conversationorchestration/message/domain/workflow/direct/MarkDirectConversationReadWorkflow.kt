package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ConversationUnseenCountRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.AcknowledgeIncomingMessageUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import kotlin.reflect.KClass

/**
 * ClearUnseenCount -> FindConversation -> FindUnreadMessages -> CheckAuthorization -> (AcknowledgeMessage)* .
 *
 * Read receipts are opportunistic: while authorization is missing (e.g. during onboarding) the
 * receipts stay pending in the database and are sent the next time the chat is read.
 */
class MarkDirectConversationReadWorkflow internal constructor(
    private val unseenCountRepository: ConversationUnseenCountRepository,
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase,
    private val acknowledgeIncomingMessage: AcknowledgeIncomingMessageUseCase
) : ConversationWorkflow<
        DirectConversationCommand.MarkConversationRead,
        MarkDirectConversationReadWorkflow.Step,
        MarkDirectConversationReadWorkflow.Outcome
    > {
    private val logger = SparrowLog.withTag("MarkDirectConversationReadWorkflow")

    override val commandType: KClass<DirectConversationCommand.MarkConversationRead> =
        DirectConversationCommand.MarkConversationRead::class

    override fun firstStep(command: DirectConversationCommand.MarkConversationRead): Step =
        Step.ClearUnseenCount(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.ClearUnseenCount -> {
                require(step.command.conversationId.isNotBlank()) { "Conversation ID must not be blank" }
                unseenCountRepository.clear(step.command.conversationId).getOrThrow()
                Outcome.UnseenCountCleared
            }
            is Step.FindConversation ->
                Outcome.ConversationFound(
                    conversationRepository.requireTarget(step.command.conversationId).getOrThrow()
                )
            is Step.FindUnreadMessages ->
                Outcome.UnreadMessagesFound(
                    target = step.target,
                    messages = messageRepository.findAwaitingReadReceipt(step.target.conversationId).getOrThrow()
                )
            is Step.CheckAuthorization -> checkAuthorization(step)
            is Step.AcknowledgeMessage -> {
                val message = step.remaining.first()
                acknowledgeIncomingMessage.forDirectMessage(message).getOrThrow()
                logger.debug {
                    "Direct read receipt queued: messageId=${message.messageId}, contactId=${message.contactId}"
                }
                Outcome.MessageAcknowledged(step.remaining.drop(1))
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.UnseenCountCleared -> Transition.Next(Step.FindConversation(step.command))
            is Outcome.ConversationFound -> Transition.Next(Step.FindUnreadMessages(outcome.target, step.command))
            is Outcome.UnreadMessagesFound -> checkUnread(outcome.target, outcome.messages, step.command)
            is Outcome.Authorized -> nextMessage(outcome.messages, step.command)
            Outcome.ReceiptsDeferred -> Transition.Complete()
            is Outcome.MessageAcknowledged -> nextMessage(outcome.remaining, step.command)
        }

    private fun checkUnread(
        target: DirectConversationTarget,
        messages: List<UnreadIncomingMessage>,
        command: DirectConversationCommand.MarkConversationRead
    ): Transition<Step> =
        if (messages.isEmpty()) {
            Transition.Complete()
        } else {
            Transition.Next(Step.CheckAuthorization(target, messages, command))
        }

    private fun nextMessage(
        remaining: List<UnreadIncomingMessage>,
        command: DirectConversationCommand.MarkConversationRead
    ): Transition<Step> =
        if (remaining.isEmpty()) Transition.Complete() else Transition.Next(Step.AcknowledgeMessage(remaining, command))

    private suspend fun checkAuthorization(step: Step.CheckAuthorization): Outcome {
        if (findDirectAuthorizationBlock(step.target.contactId).getOrThrow() == null) {
            return Outcome.Authorized(step.messages)
        }
        logger.debug { "Deferred ${step.messages.size} direct read receipts until mutual identity/keys are ready" }
        return Outcome.ReceiptsDeferred
    }

    sealed interface Step : WorkflowStep {
        val command: DirectConversationCommand.MarkConversationRead

        data class ClearUnseenCount(
            override val command: DirectConversationCommand.MarkConversationRead
        ) : Step

        data class FindConversation(
            override val command: DirectConversationCommand.MarkConversationRead
        ) : Step

        data class FindUnreadMessages(
            val target: DirectConversationTarget,
            override val command: DirectConversationCommand.MarkConversationRead
        ) : Step

        data class CheckAuthorization(
            val target: DirectConversationTarget,
            val messages: List<UnreadIncomingMessage>,
            override val command: DirectConversationCommand.MarkConversationRead
        ) : Step

        /** Sends and records the receipt for the first of the [remaining] messages. */
        data class AcknowledgeMessage(
            val remaining: List<UnreadIncomingMessage>,
            override val command: DirectConversationCommand.MarkConversationRead
        ) : Step
    }

    sealed interface Outcome {
        data object UnseenCountCleared : Outcome

        data class ConversationFound(
            val target: DirectConversationTarget
        ) : Outcome

        data class UnreadMessagesFound(
            val target: DirectConversationTarget,
            val messages: List<UnreadIncomingMessage>
        ) : Outcome

        data class Authorized(
            val messages: List<UnreadIncomingMessage>
        ) : Outcome

        data object ReceiptsDeferred : Outcome

        data class MessageAcknowledged(
            val remaining: List<UnreadIncomingMessage>
        ) : Outcome
    }
}
