package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ExpenseReceiptRepository

class StoreExpenseReceiptUseCase(
    private val receipts: ExpenseReceiptRepository
) {
    suspend operator fun invoke(groupId: String, expense: Expense): Result<Expense> =
        runCatching {
            val receipt = expense.receipt ?: return@runCatching expense
            val sourcePath = requireNotNull(receipt.localFilePath) { "Expense receipt file is missing" }
            val storedPath = receipts.import(groupId, receipt.id, sourcePath).getOrThrow()
            expense.copy(receipt = receipt.copy(localFilePath = storedPath))
        }
}
