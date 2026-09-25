package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import kotlinx.coroutines.flow.Flow

/** Read-only queries over Identity-owned persistence. */
interface RemoteIdentityReadRepository {
    suspend fun get(peerId: String): Result<RemotePeerIdentity?>

    suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?>

    fun observeAll(): Flow<List<RemotePeerIdentity>>
}
