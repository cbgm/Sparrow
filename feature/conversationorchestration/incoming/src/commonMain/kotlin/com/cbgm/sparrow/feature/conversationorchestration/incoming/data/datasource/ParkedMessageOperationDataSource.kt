package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.datasource

import com.cbgm.sparrow.data.database.dao.ParkedMessageOperationDao
import com.cbgm.sparrow.data.database.entity.ParkedMessageOperationEntity

internal class ParkedMessageOperationDataSource(
    private val dao: ParkedMessageOperationDao
) {
    suspend fun insert(operation: ParkedMessageOperationEntity) {
        dao.insert(operation)
    }

    suspend fun findByTarget(messageId: String): List<ParkedMessageOperationEntity> = dao.findByTarget(messageId)

    suspend fun delete(id: Long) {
        dao.delete(id)
    }

    suspend fun deleteParkedBefore(cutoffEpochMilliseconds: Long) {
        dao.deleteParkedBefore(cutoffEpochMilliseconds)
    }
}
