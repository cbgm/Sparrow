package com.cbgm.sparrow.feature.conversationorchestration.api.command

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseAllocation
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseCategory
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationIndicator

sealed interface GroupMessageCommand : ConversationCommand {
    val groupId: String

    data class SendMessage(
        override val groupId: String,
        val parts: List<MessagePart>,
        val replyToMessageId: String? = null
    ) : GroupMessageCommand

    data class EditMessage(
        override val groupId: String,
        val messageId: String,
        val text: String
    ) : GroupMessageCommand

    data class DeleteMessage(
        override val groupId: String,
        val messageId: String
    ) : GroupMessageCommand

    data class ToggleReaction(
        override val groupId: String,
        val messageId: String,
        val emoji: String
    ) : GroupMessageCommand

    data class VotePoll(
        override val groupId: String,
        val messageId: String,
        val pollId: String,
        val selectedOptionIds: Set<String>
    ) : GroupMessageCommand

    data class ClosePoll(
        override val groupId: String,
        val messageId: String,
        val pollId: String
    ) : GroupMessageCommand

    data class RetryMessage(
        override val groupId: String,
        val messageId: String
    ) : GroupMessageCommand

    data class MarkConversationRead(
        override val groupId: String
    ) : GroupMessageCommand

    data class SendIndicator(
        override val groupId: String,
        val indicator: ConversationIndicator
    ) : GroupMessageCommand

    data class PinMessage(
        override val groupId: String,
        val messageId: String
    ) : GroupMessageCommand

    data class UnpinMessage(
        override val groupId: String
    ) : GroupMessageCommand

    data class ActivateExpenses(
        override val groupId: String,
        val currencyCode: String
    ) : GroupMessageCommand

    data class CloseExpenses(
        override val groupId: String
    ) : GroupMessageCommand

    data class CreateExpense(
        override val groupId: String,
        val boardId: String,
        val description: String,
        val amountMinor: Long,
        val paidByMemberId: String,
        val allocations: List<ExpenseAllocation>,
        val category: ExpenseCategory
    ) : GroupMessageCommand

    data class EditExpense(
        override val groupId: String,
        val expense: Expense
    ) : GroupMessageCommand

    data class DeleteExpense(
        override val groupId: String,
        val expense: Expense
    ) : GroupMessageCommand
}
