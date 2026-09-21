package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    /** Atomic confirmation: pin the current old identity to the still-current invitation.
     * Confirmation does not update contact_public_identities, routing, or authorizations.
     */
    @Query(
        """
        UPDATE pending_remote_identity_changes
        SET fingerprintConfirmedAtEpochMilliseconds = :confirmedAt,
            confirmedPreviousEncryptionPublicKey = (
                SELECT encryptionPublicKey FROM contact_public_identities WHERE contactId = :peerId
            ),
            confirmedPreviousSigningPublicKey = (
                SELECT signingPublicKey FROM contact_public_identities WHERE contactId = :peerId
            )
        WHERE peerId = :peerId
          AND invitationId = :invitationId
          AND proposedSigningPublicKey = :proposedSigningPublicKey
          AND expiresAtEpochMilliseconds > :confirmedAt
          AND fingerprintConfirmedAtEpochMilliseconds IS NULL
          AND EXISTS (
              SELECT 1 FROM contact_public_identities AS old
              WHERE old.contactId = :peerId
                AND (old.signingPublicKey != proposedSigningPublicKey
                  OR old.encryptionPublicKey != proposedEncryptionPublicKey)
          )
          AND NOT EXISTS (
              SELECT 1 FROM contact_public_identities AS other
              WHERE other.contactId != :peerId AND other.signingPublicKey = :proposedSigningPublicKey
          )
    """
    )
    suspend fun confirmFingerprintIfCurrent(
        peerId: String,
        invitationId: String,
        proposedSigningPublicKey: ByteArray,
        confirmedAt: Long
    ): Int

    /**
     * Local-only cutover primitive. Callers must first stop old-identity delivery and
     * revoke old mailbox capabilities. A fingerprint confirmation by itself MUST
     * NOT call this method. All DB state changes are rolled back together on error.
     *
     * The existing contact row is UPDATED in place; never delete it or its chats.
     */
    @Transaction
    suspend fun replaceConfirmedIdentity(peerId: String, invitationId: String, now: Long): Boolean {
        val proposal = findByPeerId(peerId) ?: return false
        if (proposal.invitationId != invitationId ||
            proposal.expiresAtEpochMilliseconds <= now ||
            proposal.fingerprintConfirmedAtEpochMilliseconds == null ||
            proposal.confirmedPreviousEncryptionPublicKey == null ||
            proposal.confirmedPreviousSigningPublicKey == null ||
            proposal.proposedSigningPublicKey.size != 32 ||
            proposal.proposedEncryptionPublicKey.size != 32
        ) {
            return false
        }

        val changed = replaceIdentityOnlyIfConfirmationStillMatches(
            peerId = peerId,
            invitationId = invitationId,
            oldEncryptionPublicKey = proposal.confirmedPreviousEncryptionPublicKey,
            oldSigningPublicKey = proposal.confirmedPreviousSigningPublicKey,
            proposedEncryptionPublicKey = proposal.proposedEncryptionPublicKey,
            proposedSigningPublicKey = proposal.proposedSigningPublicKey,
            now = now
        )
        if (changed != 1) return false
        // Old MUTUAL / WAITING_FOR_READY exchanges must never authorize the new keys.
        invalidatePreviousExchanges(peerId, now)
        check(deleteIfInvitationMatches(peerId, invitationId) == 1) {
            "Identity-change request changed during replacement"
        }
        return true
    }

    /** Compare-and-set the precise OLD key pair captured during confirmation. */
    @Query(
        """
        UPDATE contact_public_identities
        SET encryptionPublicKey = :proposedEncryptionPublicKey,
            signingPublicKey = :proposedSigningPublicKey,
            verificationStatus = 'UNVERIFIED',
            verifiedByContact = 0,
            keyExchangeStatus = 'ONE_WAY',
            locallyImported = 0,
            remoteIdentityPacketReceived = 0,
            updatedAtEpochMilliseconds = :now
        WHERE contactId = :peerId
          AND encryptionPublicKey = :oldEncryptionPublicKey
          AND signingPublicKey = :oldSigningPublicKey
          AND EXISTS (
              SELECT 1 FROM pending_remote_identity_changes AS pending
              WHERE pending.peerId = :peerId
                AND pending.invitationId = :invitationId
                AND pending.fingerprintConfirmedAtEpochMilliseconds IS NOT NULL
                AND pending.expiresAtEpochMilliseconds > :now
                AND pending.confirmedPreviousEncryptionPublicKey = :oldEncryptionPublicKey
                AND pending.confirmedPreviousSigningPublicKey = :oldSigningPublicKey
                AND pending.proposedEncryptionPublicKey = :proposedEncryptionPublicKey
                AND pending.proposedSigningPublicKey = :proposedSigningPublicKey
          )
          AND NOT EXISTS (
              SELECT 1 FROM contact_public_identities AS other
              WHERE other.contactId != :peerId
                AND other.signingPublicKey = :proposedSigningPublicKey
          )
    """
    )
    suspend fun replaceIdentityOnlyIfConfirmationStillMatches(
        peerId: String,
        invitationId: String,
        oldEncryptionPublicKey: ByteArray,
        oldSigningPublicKey: ByteArray,
        proposedEncryptionPublicKey: ByteArray,
        proposedSigningPublicKey: ByteArray,
        now: Long
    ): Int

    @Query(
        """
        UPDATE identity_exchanges
        SET stage = 'AUTHORIZATION_REVOKED', updatedAtEpochMilliseconds = :now,
            lastError = NULL
        WHERE contactId = :peerId AND stage != 'AUTHORIZATION_REVOKED'
    """
    )
    suspend fun invalidatePreviousExchanges(peerId: String, now: Long): Int

    @Query("DELETE FROM pending_remote_identity_changes WHERE peerId = :peerId AND invitationId = :invitationId")
    suspend fun deleteIfInvitationMatches(peerId: String, invitationId: String): Int
}
