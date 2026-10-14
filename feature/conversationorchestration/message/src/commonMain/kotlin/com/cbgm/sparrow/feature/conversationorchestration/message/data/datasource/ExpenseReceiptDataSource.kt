package com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource

import com.cbgm.sparrow.data.datastore.expense.ExpenseReceiptFileStore
import com.cbgm.sparrow.data.datastore.expense.ExpenseReceiptKey

internal class ExpenseReceiptDataSource(
    private val fileStore: ExpenseReceiptFileStore
) {
    fun read(groupId: String, receiptId: String): ByteArray? =
        fileStore.read(ExpenseReceiptKey(groupId, receiptId))

    fun save(groupId: String, receiptId: String, bytes: ByteArray) =
        fileStore.write(ExpenseReceiptKey(groupId, receiptId), bytes)

    fun import(groupId: String, receiptId: String, sourcePath: String): String {
        val key = ExpenseReceiptKey(groupId, receiptId)
        fileStore.importFile(key, sourcePath)
        return fileStore.pathOf(key)
    }

    fun delete(groupId: String, receiptIds: Collection<String>) =
        receiptIds.forEach { receiptId -> fileStore.delete(ExpenseReceiptKey(groupId, receiptId)) }

    fun deleteGroup(groupId: String) = fileStore.deleteGroup(groupId)
}
