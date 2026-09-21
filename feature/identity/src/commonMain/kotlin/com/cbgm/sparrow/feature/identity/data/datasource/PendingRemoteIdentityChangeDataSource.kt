package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.database.dao.PendingRemoteIdentityChangeDao
import com.cbgm.sparrow.data.database.entity.PendingRemoteIdentityChangeEntity
import kotlinx.coroutines.flow.Flow

internal class PendingRemoteIdentityChangeDataSource(
    private val dao: PendingRemoteIdentityChangeDao
) {
    suspend fun find(peerId: String): PendingRemoteIdentityChangeEntity? = dao.findByPeerId(peerId)

    suspend fun upsert(candidate: PendingRemoteIdentityChangeEntity) = dao.upsert(candidate)

    fun observeAll(): Flow<List<PendingRemoteIdentityChangeEntity>> = dao.observeAll()

    suspend fun discard(peerId: String, invitationId: String) = dao.deleteIfInvitationMatches(peerId, invitationId)

    suspend fun confirmFingerprintIfCurrent(
        peerId: String,
        invitationId: String,
        signingKey: ByteArray,
        encryptionKey: ByteArray,
        now: Long
    ): Int = dao.confirmFingerprintIfCurrent(peerId, invitationId, signingKey, encryptionKey, now)

    /** The DAO performs the key swap, exchange invalidation and packet quarantine atomically. */
    suspend fun replaceConfirmedIdentity(peerId: String, invitationId: String, now: Long): Boolean =
        dao.replaceConfirmedIdentity(peerId, invitationId, now)
}
