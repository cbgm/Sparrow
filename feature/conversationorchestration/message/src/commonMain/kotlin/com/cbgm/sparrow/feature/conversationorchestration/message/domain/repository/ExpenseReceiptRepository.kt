package com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository

interface ExpenseReceiptRepository {
    suspend fun read(groupId: String, receiptId: String): Result<ByteArray?>

    suspend fun save(groupId: String, receiptId: String, bytes: ByteArray): Result<Unit>

    suspend fun import(groupId: String, receiptId: String, sourcePath: String): Result<String>

    suspend fun delete(groupId: String, receiptIds: Collection<String>): Result<Unit>

    suspend fun deleteGroup(groupId: String): Result<Unit>
}
