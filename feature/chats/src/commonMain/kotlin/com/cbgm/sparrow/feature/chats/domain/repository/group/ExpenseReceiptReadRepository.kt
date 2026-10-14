package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.core.messagepart.domain.model.Expense

interface ExpenseReceiptReadRepository {
    fun withLocalReceipts(groupId: String, expenses: List<Expense>): List<Expense>
}
