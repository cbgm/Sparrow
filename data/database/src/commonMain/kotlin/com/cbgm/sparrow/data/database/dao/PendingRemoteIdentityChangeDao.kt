package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingRemoteIdentityChangeDao {
    @Upsert
    suspend fun upsert(candidate: PendingRemoteIdentityChangeEntity)

    @Query("SELECT * FROM pending_remote_identity_changes WHERE peerId = :peerId LIMIT 1")
    suspend fun findByPeerId(peerId: String): PendingRemoteIdentityChangeEntity?

    @Query("SELECT * FROM pending_remote_identity_changes ORDER BY receivedAtEpochMilliseconds DESC")
    fun observeAll(): Flow<List<PendingRemoteIdentityChangeEntity>>

    @Query("DELETE FROM pending_remote_identity_changes WHERE peerId = :peerId AND invitationId = :invitationId")
    suspend fun deleteIfInvitationMatches(peerId: String, invitationId: String): Int
}
