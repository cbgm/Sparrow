package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.datastore.expense.ExpenseReceiptFileStore
import com.cbgm.sparrow.data.datastore.expense.ExpenseReceiptKey

internal class ExpenseReceiptDataSource(
    private val fileStore: ExpenseReceiptFileStore
) {
    fun localPath(groupId: String, receiptId: String): String? {
        val key = ExpenseReceiptKey(groupId, receiptId)
        return fileStore.pathOf(key).takeIf { fileStore.exists(key) }
    }
}
