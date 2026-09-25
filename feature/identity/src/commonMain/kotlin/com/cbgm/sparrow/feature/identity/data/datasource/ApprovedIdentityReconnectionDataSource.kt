package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.database.dao.ApprovedIdentityReconnectionDao
import com.cbgm.sparrow.data.database.entity.ApprovedIdentityReconnectionEntity
import kotlinx.coroutines.flow.Flow

internal class ApprovedIdentityReconnectionDataSource(
    private val dao: ApprovedIdentityReconnectionDao
) {
    fun observeAll(): Flow<List<ApprovedIdentityReconnectionEntity>> = dao.observeAll()

    suspend fun find(peerId: String): ApprovedIdentityReconnectionEntity? = dao.find(peerId)

    suspend fun acknowledgeQueued(peerId: String, approvalId: String) = dao.deleteIfCurrent(peerId, approvalId)
}
