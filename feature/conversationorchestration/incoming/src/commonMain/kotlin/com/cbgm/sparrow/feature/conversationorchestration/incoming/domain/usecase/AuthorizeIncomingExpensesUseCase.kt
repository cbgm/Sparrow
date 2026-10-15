package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransfer
import com.cbgm.sparrow.core.messagepart.domain.model.PaymentTransferPolicy
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.IncomingExpensePolicy
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.first

class AuthorizeIncomingExpensesUseCase(
    private val membership: GroupMembershipRepository,
    private val attachments: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(
        groupId: String,
        senderContactId: String,
        parts: List<MessagePart>
    ): Result<Unit> = safeSuspendCall(logFailure = false) {
        val expenses = parts.filterIsInstance<Expense>()
        val transfers = parts.filterIsInstance<PaymentTransfer>()
        if (expenses.isEmpty() && transfers.isEmpty()) return@safeSuspendCall

        val senderKey = membership.getCurrentTransportRoutingMembers(groupId).getOrThrow().orEmpty()
            .firstOrNull { it.contactId == senderContactId }
            ?.signingPublicKey
            ?: error("Expense sender is not a group member")
        val senderId = ExpenseMemberId.fromSigningPublicKey(senderKey)
        val senderIsAdmin = senderContactId in membership.observeAdministration(groupId).first().adminContactIds
        val stored = attachments.observeExpenses(groupId).first()
        val closedBoardIds =
            attachments.observeExpenseBoardClosures(groupId).first().mapTo(mutableSetOf()) { it.boardId }

        val storedTransfers = if (transfers.isEmpty()) emptyList() else attachments.observePaymentTransfers(groupId).first()
        transfers.forEach { event ->
            require(storedTransfers.none { it.id == event.id && it != event }) { "Conflicting payment event" }
            require(event.boardId !in closedBoardIds) { "Payment belongs to a closed expense board" }
            PaymentTransferPolicy.requireActor(event, senderId)
        }
        expenses.forEach { expense ->
            require(expense.boardId !in closedBoardIds) { "Expense belongs to a closed expense board" }
            IncomingExpensePolicy.requireAuthorized(expense, stored, senderId, senderIsAdmin)
        }
    }
}
