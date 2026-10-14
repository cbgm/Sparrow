package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.latestRevision
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import kotlinx.coroutines.flow.first

class FindCurrentExpenseRevisionUseCase(
    private val attachments: MessageAttachmentOperationsRepository
) {
    suspend operator fun invoke(groupId: String, expenseId: String): Result<Expense?> =
        runCatching { attachments.observeExpenses(groupId).first().latestRevision(expenseId) }
}
