package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import kotlinx.coroutines.flow.Flow

interface PendingRemoteIdentityChangeRepository {
    suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit>

    fun observeAll(): Flow<List<PendingRemoteIdentityChange>>

    suspend fun discard(peerId: String, invitationId: String): Result<Unit>
}
