package com.cbgm.sparrow.feature.conversationorchestration.incoming.data.datasource

import com.cbgm.sparrow.data.database.dao.ParkedEnvelopeDao
import com.cbgm.sparrow.data.database.entity.ParkedEnvelopeEntity

internal class ParkedEnvelopeDataSource(
    private val dao: ParkedEnvelopeDao
) {
    suspend fun park(envelope: ParkedEnvelopeEntity) {
        dao.park(envelope)
    }

    suspend fun findWaitingKeys(): List<String> = dao.findWaitingKeys()

    suspend fun findByWaitingKeys(waitingKeys: List<String>): List<ParkedEnvelopeEntity> =
        dao.findByWaitingKeys(waitingKeys)

    suspend fun delete(envelopeId: String) {
        dao.delete(envelopeId)
    }

    suspend fun deleteParkedBefore(cutoffEpochMilliseconds: Long) {
        dao.deleteParkedBefore(cutoffEpochMilliseconds)
    }
}
