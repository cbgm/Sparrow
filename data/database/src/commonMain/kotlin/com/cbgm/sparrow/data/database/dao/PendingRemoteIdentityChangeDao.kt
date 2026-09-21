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

    @Query("DELETE FROM pending_remote_identity_changes WHERE peerId = :peerId AND invitationId = :invitationId")
    suspend fun deleteIfInvitationMatches(peerId: String, invitationId: String): Int
}
