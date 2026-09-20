package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.RemoteIdentityDao
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.feature.identity.data.model.RemoteIdentityImportOriginDto
import com.cbgm.sparrow.feature.identity.data.model.StoredKeyExchangeStatusDto
import com.cbgm.sparrow.feature.identity.data.model.StoredVerificationStatusDto
import kotlinx.coroutines.flow.Flow

internal class RemoteIdentityDataSource(
    private val remoteIdentityDao: RemoteIdentityDao,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle
) {
    fun observeAll(): Flow<List<ContactPublicIdentityEntity>> = remoteIdentityDao.observeAll()

    suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): ContactPublicIdentityEntity? {
        require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
        return remoteIdentityDao.findBySigningPublicKey(signingPublicKey)
    }

    suspend fun findByPeerId(peerId: String): ContactPublicIdentityEntity? {
        require(peerId.isNotBlank()) { "Peer ID must not be blank" }
        return remoteIdentityDao.findByPeerId(peerId)
    }

    suspend fun stage(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Boolean {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)

        val existing = remoteIdentityDao.findByPeerId(peerId)
        val sameIdentity =
            existing != null &&
                existing.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                existing.signingPublicKey.contentEquals(signingPublicKey)
        val identityChanged = existing != null && !sameIdentity

        if (identityChanged) {
            val identityIsPinned =
                existing.keyExchangeStatus == StoredKeyExchangeStatusDto.MUTUAL.name ||
                    existing.verificationStatus == StoredVerificationStatusDto.VERIFIED.name
            check(!identityIsPinned) {
                "Stored mutual or verified identity cannot be replaced without an explicit reset"
            }
            mailboxCapabilityLifecycle.revokeForContact(peerId).getOrThrow()
        }

        if (sameIdentity && existing != null) {
            remoteIdentityDao.upsert(
                existing.copy(
                    remoteIdentityPacketReceived = true,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            )
            return false
        }

        remoteIdentityDao.upsert(
            ContactPublicIdentityEntity(
                contactId = peerId,
                encryptionPublicKey = encryptionPublicKey.copyOf(),
                signingPublicKey = signingPublicKey.copyOf(),
                verificationStatus = StoredVerificationStatusDto.UNVERIFIED.name,
                verifiedByContact = false,
                keyExchangeStatus = StoredKeyExchangeStatusDto.ONE_WAY.name,
                locallyImported = false,
                remoteIdentityPacketReceived = true,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        )
        return identityChanged
    }

    /** Preserve the legacy QR/manual import semantics, including verified and pinned identities. */
    suspend fun storeImported(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityImportOriginDto
    ): Pair<ContactPublicIdentityEntity, Boolean> {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)
        val existing = remoteIdentityDao.findByPeerId(peerId)
        val sameIdentity = existing != null &&
            existing.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
            existing.signingPublicKey.contentEquals(signingPublicKey)
        val identityChanged = existing != null && !sameIdentity
        if (identityChanged) {
            val previous = requireNotNull(existing)
            check(
                previous.keyExchangeStatus != StoredKeyExchangeStatusDto.MUTUAL.name &&
                    previous.verificationStatus != StoredVerificationStatusDto.VERIFIED.name
            ) { "Stored mutual or verified identity cannot be replaced without an explicit reset" }
            mailboxCapabilityLifecycle.revokeForContact(peerId).getOrThrow()
        }
        val locallyImported = (sameIdentity && existing.locallyImported == true) ||
            origin == RemoteIdentityImportOriginDto.LOCAL_IMPORT || origin == RemoteIdentityImportOriginDto.TRUSTED_QR_IMPORT
        val packetReceived = (sameIdentity && existing.remoteIdentityPacketReceived == true) ||
            origin == RemoteIdentityImportOriginDto.REMOTE_PACKET || origin == RemoteIdentityImportOriginDto.REMOTE_HANDSHAKE
        val exchangeStatus = when {
            sameIdentity && existing.keyExchangeStatus == StoredKeyExchangeStatusDto.MUTUAL.name -> StoredKeyExchangeStatusDto.MUTUAL
            origin == RemoteIdentityImportOriginDto.REMOTE_HANDSHAKE -> StoredKeyExchangeStatusDto.ONE_WAY
            locallyImported && packetReceived -> StoredKeyExchangeStatusDto.MUTUAL
            else -> StoredKeyExchangeStatusDto.ONE_WAY
        }
        val verificationStatus = when {
            origin == RemoteIdentityImportOriginDto.TRUSTED_QR_IMPORT -> StoredVerificationStatusDto.VERIFIED
            sameIdentity && existing?.verificationStatus == StoredVerificationStatusDto.VERIFIED.name -> StoredVerificationStatusDto.VERIFIED
            else -> StoredVerificationStatusDto.UNVERIFIED
        }
        val next = ContactPublicIdentityEntity(
            contactId = peerId,
            encryptionPublicKey = encryptionPublicKey.copyOf(),
            signingPublicKey = signingPublicKey.copyOf(),
            verificationStatus = verificationStatus.name,
            verifiedByContact = sameIdentity && existing?.verifiedByContact == true,
            keyExchangeStatus = exchangeStatus.name,
            locallyImported = locallyImported,
            remoteIdentityPacketReceived = packetReceived,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
        remoteIdentityDao.upsert(next)
        return next to identityChanged
    }

    /** A manual acceptance can promote to mutual only when the stored keys match. */
    suspend fun acceptImported(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)
        check(
            remoteIdentityDao.markLocallyImportedIfKeysMatch(
                peerId = peerId,
                expectedEncryptionPublicKey = encryptionPublicKey,
                expectedSigningPublicKey = signingPublicKey,
                oneWayStatus = StoredKeyExchangeStatusDto.ONE_WAY.name,
                mutualStatus = StoredKeyExchangeStatusDto.MUTUAL.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            ) == 1
        ) { "Contact identity changed before remote identity acceptance was applied" }
    }

    /** Handshake acceptance is compare-and-set: a key change must fail, never replace identity. */
    suspend fun acceptStoredHandshake(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)
        val existing = remoteIdentityDao.findByPeerId(peerId)
        val alreadyMutual = existing != null &&
            existing.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
            existing.signingPublicKey.contentEquals(signingPublicKey) &&
            existing.keyExchangeStatus == StoredKeyExchangeStatusDto.MUTUAL.name
        if (alreadyMutual) return

        val updatedRows = remoteIdentityDao.markLocallyAcceptedForHandshakeIfKeysMatch(
            peerId = peerId,
            expectedEncryptionPublicKey = encryptionPublicKey,
            expectedSigningPublicKey = signingPublicKey,
            oneWayStatus = StoredKeyExchangeStatusDto.ONE_WAY.name,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
        check(updatedRows == 1) { "Contact identity changed before handshake acceptance was recorded" }
    }

    suspend fun recordRemotePacket(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): StoredKeyExchangeStatusDto {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)
        val existing = remoteIdentityDao.findByPeerId(peerId) ?: error("Remote identity was not found")
        check(existing.encryptionPublicKey.contentEquals(encryptionPublicKey)) {
            "Remote encryption key does not match the imported identity"
        }
        check(existing.signingPublicKey.contentEquals(signingPublicKey)) {
            "Remote signing key does not match the imported identity"
        }
        val nextStatus = if (existing.locallyImported) StoredKeyExchangeStatusDto.MUTUAL else StoredKeyExchangeStatusDto.ONE_WAY
        remoteIdentityDao.upsert(
            existing.copy(
                remoteIdentityPacketReceived = true,
                keyExchangeStatus = nextStatus.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        )
        return nextStatus
    }

    suspend fun accept(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)

        val existing = remoteIdentityDao.findByPeerId(peerId)
        val sameIdentity =
            existing != null &&
                existing.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
                existing.signingPublicKey.contentEquals(signingPublicKey)
        val alreadyMutual = sameIdentity && existing?.keyExchangeStatus == StoredKeyExchangeStatusDto.MUTUAL.name
        if (alreadyMutual) return

        if (!sameIdentity) {
            if (existing != null) {
                // The same trust invariant must hold for every acceptance path, not just
                // stage()/storeImported(). A handshake must never silently rotate a
                // verified or mutually established peer identity.
                check(
                    existing.keyExchangeStatus != StoredKeyExchangeStatusDto.MUTUAL.name &&
                        existing.verificationStatus != StoredVerificationStatusDto.VERIFIED.name
                ) {
                    "Stored mutual or verified identity cannot be replaced without an explicit reset"
                }
                mailboxCapabilityLifecycle.revokeForContact(peerId).getOrThrow()
            }
            remoteIdentityDao.upsert(
                ContactPublicIdentityEntity(
                    contactId = peerId,
                    encryptionPublicKey = encryptionPublicKey.copyOf(),
                    signingPublicKey = signingPublicKey.copyOf(),
                    verificationStatus = StoredVerificationStatusDto.UNVERIFIED.name,
                    verifiedByContact = false,
                    keyExchangeStatus = StoredKeyExchangeStatusDto.ONE_WAY.name,
                    locallyImported = true,
                    remoteIdentityPacketReceived = true,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            )
            return
        }

        val updatedRows =
            remoteIdentityDao.markLocallyAcceptedForHandshakeIfKeysMatch(
                peerId = peerId,
                expectedEncryptionPublicKey = encryptionPublicKey,
                expectedSigningPublicKey = signingPublicKey,
                oneWayStatus = StoredKeyExchangeStatusDto.ONE_WAY.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        check(updatedRows == 1) {
            "Remote identity changed before handshake acceptance was recorded"
        }
    }

    suspend fun markMutual(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        requirePeerAndKeys(peerId, encryptionPublicKey, signingPublicKey)
        val updatedRows =
            remoteIdentityDao.updateKeyExchangeStatusIfKeysMatch(
                peerId = peerId,
                expectedEncryptionPublicKey = encryptionPublicKey,
                expectedSigningPublicKey = signingPublicKey,
                keyExchangeStatus = StoredKeyExchangeStatusDto.MUTUAL.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        check(updatedRows == 1) {
            "Remote identity changed before acknowledgement was applied"
        }
    }

    suspend fun ensureSigningIdentityMatches(
        peerId: String,
        signingPublicKey: ByteArray
    ) {
        require(peerId.isNotBlank()) { "Peer ID must not be blank" }
        require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
        val existing = remoteIdentityDao.findByPeerId(peerId) ?: return
        check(existing.signingPublicKey.contentEquals(signingPublicKey)) {
            "Remote signing identity conflicts with the handshake"
        }
    }

    private fun requirePeerAndKeys(
        peerId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        require(peerId.isNotBlank()) { "Peer ID must not be blank" }
        require(encryptionPublicKey.isNotEmpty()) { "Encryption public key must not be empty" }
        require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
    }
}
