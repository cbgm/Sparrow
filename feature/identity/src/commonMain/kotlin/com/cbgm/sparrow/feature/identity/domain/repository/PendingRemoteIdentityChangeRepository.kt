package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import kotlinx.coroutines.flow.Flow

interface PendingRemoteIdentityChangeRepository {
    suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit>

    fun observeAll(): Flow<List<PendingRemoteIdentityChange>>

    suspend fun discard(peerId: String, invitationId: String): Result<Unit>

    /** Explicit approval of exactly the public keys displayed in Mailbox. This is a user
     * decision to trust new keys, NOT proof that the old identity controls them.
     * The signature on the incoming invitation is verified before it is staged.
     * Never silently promote a request based on a matching phone number.
     */
    suspend fun approveReplacement(
        peerId: String,
        invitationId: String,
        presentedSigningFingerprint: String,
        presentedEncryptionFingerprint: String
    ): Result<Unit>
}
