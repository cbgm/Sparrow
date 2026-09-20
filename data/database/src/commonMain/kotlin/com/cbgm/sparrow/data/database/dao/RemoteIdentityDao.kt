package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import kotlinx.coroutines.flow.Flow

/** Persistence for remote cryptographic identities. Owned by feature:identity. */
@Dao
interface RemoteIdentityDao {
    @Query("SELECT * FROM contact_public_identities ORDER BY contactId")
    fun observeAll(): Flow<List<ContactPublicIdentityEntity>>

    @Upsert
    suspend fun upsert(identity: ContactPublicIdentityEntity)

    @Query(
        """
        SELECT *
        FROM contact_public_identities
        WHERE contactId = :peerId
        LIMIT 1
        """
    )
    suspend fun findByPeerId(peerId: String): ContactPublicIdentityEntity?

    @Query(
        """
        SELECT *
        FROM contact_public_identities
        WHERE signingPublicKey = :signingPublicKey
        LIMIT 1
        """
    )
    suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): ContactPublicIdentityEntity?

    @Query(
        """
        UPDATE contact_public_identities
        SET locallyImported = 1,
            keyExchangeStatus = CASE
                WHEN remoteIdentityPacketReceived = 1 THEN :mutualStatus
                ELSE :oneWayStatus
            END,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE contactId = :peerId
          AND encryptionPublicKey = :expectedEncryptionPublicKey
          AND signingPublicKey = :expectedSigningPublicKey
        """
    )
    suspend fun markLocallyImportedIfKeysMatch(
        peerId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        oneWayStatus: String,
        mutualStatus: String,
        updatedAtEpochMilliseconds: Long
    ): Int

    @Query(
        """
        UPDATE contact_public_identities
        SET locallyImported = 1,
            keyExchangeStatus = :oneWayStatus,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE contactId = :peerId
          AND encryptionPublicKey = :expectedEncryptionPublicKey
          AND signingPublicKey = :expectedSigningPublicKey
        """
    )
    suspend fun markLocallyAcceptedForHandshakeIfKeysMatch(
        peerId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        oneWayStatus: String,
        updatedAtEpochMilliseconds: Long
    ): Int

    @Query(
        """
        UPDATE contact_public_identities
        SET keyExchangeStatus = :keyExchangeStatus,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE contactId = :peerId
          AND encryptionPublicKey = :expectedEncryptionPublicKey
          AND signingPublicKey = :expectedSigningPublicKey
          AND locallyImported = 1
          AND remoteIdentityPacketReceived = 1
        """
    )
    suspend fun updateKeyExchangeStatusIfKeysMatch(
        peerId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        keyExchangeStatus: String,
        updatedAtEpochMilliseconds: Long
    ): Int

    @Query(
        """
        UPDATE contact_public_identities
        SET verificationStatus = :verificationStatus,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE contactId = :contactId
          AND encryptionPublicKey = :expectedEncryptionPublicKey
          AND signingPublicKey = :expectedSigningPublicKey
        """
    )
    suspend fun updateVerificationStatusIfKeysMatch(
        contactId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        verificationStatus: String,
        updatedAtEpochMilliseconds: Long
    ): Int

    @Query(
        """
        UPDATE contact_public_identities
        SET verifiedByContact = 1,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE contactId = :contactId
          AND encryptionPublicKey = :expectedEncryptionPublicKey
          AND signingPublicKey = :expectedSigningPublicKey
          AND keyExchangeStatus = :mutualStatus
        """
    )
    suspend fun markVerifiedByContactIfKeysMatch(
        contactId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        mutualStatus: String,
        updatedAtEpochMilliseconds: Long
    ): Int
}
