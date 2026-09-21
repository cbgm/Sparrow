package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import kotlinx.coroutines.flow.Flow

interface PendingRemoteIdentityChangeRepository {
    suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit>

    fun observeAll(): Flow<List<PendingRemoteIdentityChange>>

    suspend fun discard(peerId: String, invitationId: String): Result<Unit>

    /** Records an explicit, out-of-band fingerprint confirmation; does NOT replace any keys.

     * Only an independently confirmed request may proceed. Revoke old mailbox routes first.
     * This does NOT authorize a conversation or mark the new keys as verified.
     */
    suspend fun approveReplacement(peerId: String, invitationId: String): Result<Unit>

    suspend fun confirmFingerprint(
        peerId: String,
        invitationId: String,
        independentlyCheckedSigningFingerprint: String
    ): Result<Unit>
}
