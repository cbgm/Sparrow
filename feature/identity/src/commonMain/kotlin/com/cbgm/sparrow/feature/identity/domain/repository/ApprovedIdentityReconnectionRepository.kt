package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import kotlinx.coroutines.flow.Flow

interface ApprovedIdentityReconnectionRepository {
    fun observeAll(): Flow<List<ApprovedIdentityReconnection>>

    suspend fun find(peerId: String): Result<ApprovedIdentityReconnection?>

    suspend fun acknowledgeQueued(peerId: String, approvalId: String): Result<Unit>
}
