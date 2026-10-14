package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.feature.chats.data.group.datasource.ExpenseReceiptDataSource
import com.cbgm.sparrow.feature.chats.domain.repository.group.ExpenseReceiptReadRepository

internal class ExpenseReceiptReadRepositoryImpl(
    private val dataSource: ExpenseReceiptDataSource
) : ExpenseReceiptReadRepository {
    override fun withLocalReceipts(groupId: String, expenses: List<Expense>): List<Expense> =
        expenses.map { expense ->
            val receipt = expense.receipt ?: return@map expense
            val localFilePath = dataSource.localPath(groupId, receipt.id)
            expense.copy(receipt = receipt.copy(localFilePath = localFilePath))
        }
}
