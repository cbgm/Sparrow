package com.cbgm.sparrow.feature.identity.domain.repository

import kotlinx.coroutines.flow.Flow

/** Local record that we initiated sharing our current identity with this peer.
 * This is NOT proof that the peer imported it, nor an encryption/trust signal.
 */
interface LocalIdentitySharingRepository {
    fun observeSharedWith(peerId: String): Flow<Boolean>

    suspend fun recordSharedWith(peerId: String)
}
