package com.cbgm.sparrow.feature.conversationorchestration.message.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.data.datasource.ExpenseReceiptDataSource
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ExpenseReceiptRepository

internal class ExpenseReceiptRepositoryImpl(
    private val dataSource: ExpenseReceiptDataSource
) : ExpenseReceiptRepository {
    override suspend fun read(groupId: String, receiptId: String): Result<ByteArray?> =
        safeSuspendCall { dataSource.read(groupId, receiptId) }

    override suspend fun save(groupId: String, receiptId: String, bytes: ByteArray): Result<Unit> =
        safeSuspendCall { dataSource.save(groupId, receiptId, bytes) }

    override suspend fun import(groupId: String, receiptId: String, sourcePath: String): Result<String> =
        safeSuspendCall { dataSource.import(groupId, receiptId, sourcePath) }

    override suspend fun delete(groupId: String, receiptIds: Collection<String>): Result<Unit> =
        safeSuspendCall { dataSource.delete(groupId, receiptIds) }

    override suspend fun deleteGroup(groupId: String): Result<Unit> =
        safeSuspendCall { dataSource.deleteGroup(groupId) }
}
