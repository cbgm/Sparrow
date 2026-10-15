package com.cbgm.sparrow.feature.conversationorchestration.message.domain.workflow.direct

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.command.DirectConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectConversationTarget
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.UnreadIncomingMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ConversationUnseenCountRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.FindDirectAuthorizationBlockUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.SendReadReceiptUseCase
import kotlin.reflect.KClass

/**
 * ClearUnseenCount -> FindConversation -> FindUnreadMessages -> CheckAuthorization
 * -> (SendReadReceipt -> MarkReceiptSent)* .
 *
 * Read receipts are opportunistic: while authorization is missing (e.g. during onboarding) the
 * receipts stay pending in the database and are sent the next time the chat is read.
 */
class MarkDirectConversationReadWorkflow internal constructor(
    private val unseenCountRepository: ConversationUnseenCountRepository,
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository,
    private val findDirectAuthorizationBlock: FindDirectAuthorizationBlockUseCase,
    private val sendReadReceipt: SendReadReceiptUseCase
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
            is Step.CheckAuthorization ->
                if (findDirectAuthorizationBlock(step.target.contactId).getOrThrow() == null) {
                    Outcome.Authorized(step.messages)
                } else {
                    logger.debug {
                        "Deferred ${step.messages.size} direct read receipts until mutual identity/keys are ready"
                    }
                    Outcome.ReceiptsDeferred
                }
            is Step.SendReadReceipt -> {
                val message = step.remaining.first()
                sendReadReceipt(message.messageId, message.contactId).getOrThrow()
                Outcome.ReadReceiptSent(step.remaining)
            }
            is Step.MarkReceiptSent -> {
                val message = step.remaining.first()
                check(messageRepository.markReadReceiptSent(message.messageId).getOrThrow()) {
                    "Incoming direct message could not be marked as read"
                }
                logger.debug {
                    "Direct read receipt queued: messageId=${message.messageId}, contactId=${message.contactId}"
                }
                Outcome.ReceiptMarked(step.remaining.drop(1))
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.UnseenCountCleared -> Transition.Next(Step.FindConversation(step.command))
            is Outcome.ConversationFound -> Transition.Next(Step.FindUnreadMessages(outcome.target, step.command))
            is Outcome.UnreadMessagesFound ->
                if (outcome.messages.isEmpty()) {
                    Transition.Complete()
                } else {
                    Transition.Next(Step.CheckAuthorization(outcome.target, outcome.messages, step.command))
                }
            is Outcome.Authorized -> Transition.Next(Step.SendReadReceipt(outcome.messages, step.command))
            Outcome.ReceiptsDeferred -> Transition.Complete()
            is Outcome.ReadReceiptSent -> Transition.Next(Step.MarkReceiptSent(outcome.remaining, step.command))
            is Outcome.ReceiptMarked ->
                if (outcome.remaining.isEmpty()) {
                    Transition.Complete()
                } else {
                    Transition.Next(Step.SendReadReceipt(outcome.remaining, step.command))
                }
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

        /** Sends the receipt for the first of the [remaining] messages. */
        data class SendReadReceipt(
            val remaining: List<UnreadIncomingMessage>,
            override val command: DirectConversationCommand.MarkConversationRead
        ) : Step

        data class MarkReceiptSent(
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

        data class ReadReceiptSent(
            val remaining: List<UnreadIncomingMessage>
        ) : Outcome

        data class ReceiptMarked(
            val remaining: List<UnreadIncomingMessage>
        ) : Outcome
    }
}
