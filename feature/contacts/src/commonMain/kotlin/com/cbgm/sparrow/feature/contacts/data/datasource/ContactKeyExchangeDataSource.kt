package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityUpdate

class ContactKeyExchangeDataSource(
    private val contactDao: ContactDao,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) {
    suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val existing = contactDao.findPublicIdentityByContactId(contactId = contactId)

            val sameEncryptionKey =
                existing?.encryptionPublicKey?.contentEquals(encryptionPublicKey) ?: false

            val sameSigningKey =
                existing?.signingPublicKey?.contentEquals(signingPublicKey) ?: false

            val sameIdentity = existing != null && sameEncryptionKey && sameSigningKey

            val identityChanged = existing != null && !sameIdentity

            if (identityChanged) {
                val identityIsPinned =
                    existing.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name ||
                        existing.verificationStatus == ContactVerificationStatus.VERIFIED.name

                check(!identityIsPinned) {
                    "Stored mutual or verified identity cannot be replaced without an explicit reset"
                }
                mailboxCapabilityLifecycle.revokeForContact(contactId).getOrThrow()
            }

            val nextLocallyImported =
                sameIdentity && existing.locallyImported || origin == RemoteIdentityOrigin.LOCAL_IMPORT || origin == RemoteIdentityOrigin.TRUSTED_QR_IMPORT

            val nextRemoteIdentityPacketReceived =
                sameIdentity && existing.remoteIdentityPacketReceived || origin == RemoteIdentityOrigin.REMOTE_PACKET || origin == RemoteIdentityOrigin.CONTACT_INVITATION

            val nextKeyExchangeStatus =
                when {
                    sameIdentity && existing.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name -> {
                        KeyExchangeStatus.MUTUAL
                    }

                    origin == RemoteIdentityOrigin.CONTACT_INVITATION -> {
                        KeyExchangeStatus.ONE_WAY
                    }

                    nextLocallyImported && nextRemoteIdentityPacketReceived -> {
                        KeyExchangeStatus.MUTUAL
                    }

                    else -> {
                        KeyExchangeStatus.ONE_WAY
                    }
                }

            val nextVerificationStatus =
                when {
                    origin == RemoteIdentityOrigin.TRUSTED_QR_IMPORT -> {
                        ContactVerificationStatus.VERIFIED
                    }

                    sameIdentity &&
                        existing.verificationStatus == ContactVerificationStatus.VERIFIED.name -> {
                        ContactVerificationStatus.VERIFIED
                    }

                    else -> {
                        ContactVerificationStatus.UNVERIFIED
                    }
                }

            contactDao.upsertPublicIdentity(
                identity =
                    ContactPublicIdentityEntity(
                        contactId = contactId,
                        encryptionPublicKey = encryptionPublicKey.copyOf(),
                        signingPublicKey = signingPublicKey.copyOf(),
                        verificationStatus = nextVerificationStatus.name,
                        verifiedByContact = sameIdentity && existing.verifiedByContact,
                        keyExchangeStatus = nextKeyExchangeStatus.name,
                        locallyImported = nextLocallyImported,
                        remoteIdentityPacketReceived = nextRemoteIdentityPacketReceived,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
            )

            RemoteIdentityUpdate(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey.copyOf(),
                signingPublicKey = signingPublicKey.copyOf(),
                keyExchangeStatus = nextKeyExchangeStatus,
                verificationStatus = nextVerificationStatus,
                identityChanged = identityChanged
            )
        }

    suspend fun acceptRemoteIdentity(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(expectedRemoteEncryptionPublicKey.isNotEmpty()) {
                "Expected encryption key must not be empty"
            }

            require(expectedRemoteSigningPublicKey.isNotEmpty()) {
                "Expected signing key must not be empty"
            }

            val updatedRows =
                contactDao.markLocallyImportedIfKeysMatch(
                    contactId = contactId,
                    expectedEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
                    expectedSigningPublicKey = expectedRemoteSigningPublicKey,
                    oneWayStatus = KeyExchangeStatus.ONE_WAY.name,
                    mutualStatus = KeyExchangeStatus.MUTUAL.name,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )

            check(updatedRows == 1) {
                "Contact identity changed before invitation acceptance was applied"
            }
        }

    suspend fun acceptRemoteIdentityForHandshake(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(expectedRemoteEncryptionPublicKey.isNotEmpty()) {
                "Expected encryption key must not be empty"
            }

            require(expectedRemoteSigningPublicKey.isNotEmpty()) {
                "Expected signing key must not be empty"
            }

            val existingIdentity = contactDao.findPublicIdentityByContactId(contactId)
            val isAlreadyMutual =
                existingIdentity != null &&
                    existingIdentity.encryptionPublicKey.contentEquals(
                        expectedRemoteEncryptionPublicKey
                    ) &&
                    existingIdentity.signingPublicKey.contentEquals(expectedRemoteSigningPublicKey) &&
                    existingIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name
            if (isAlreadyMutual) {
                return@runCatching
            }

            val updatedRows =
                contactDao.markLocallyAcceptedForHandshakeIfKeysMatch(
                    contactId = contactId,
                    expectedEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
                    expectedSigningPublicKey = expectedRemoteSigningPublicKey,
                    oneWayStatus = KeyExchangeStatus.ONE_WAY.name,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )

            check(updatedRows == 1) {
                "Contact identity changed before invitation acceptance was recorded"
            }
        }

    suspend fun acceptInvitationIdentityForHandshake(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }
            require(remoteEncryptionPublicKey.isNotEmpty()) {
                "Remote encryption key must not be empty"
            }
            require(remoteSigningPublicKey.isNotEmpty()) {
                "Remote signing key must not be empty"
            }

            val existing = contactDao.findPublicIdentityByContactId(contactId)
            val sameIdentity =
                existing != null &&
                    existing.encryptionPublicKey.contentEquals(remoteEncryptionPublicKey) &&
                    existing.signingPublicKey.contentEquals(remoteSigningPublicKey)

            if (sameIdentity) {
                acceptRemoteIdentityForHandshake(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey = remoteEncryptionPublicKey,
                    expectedRemoteSigningPublicKey = remoteSigningPublicKey
                ).getOrThrow()
                return@runCatching
            }

            if (existing != null) {
                mailboxCapabilityLifecycle.revokeForContact(contactId).getOrThrow()
            }

            contactDao.upsertPublicIdentity(
                ContactPublicIdentityEntity(
                    contactId = contactId,
                    encryptionPublicKey = remoteEncryptionPublicKey.copyOf(),
                    signingPublicKey = remoteSigningPublicKey.copyOf(),
                    verificationStatus = ContactVerificationStatus.UNVERIFIED.name,
                    verifiedByContact = false,
                    keyExchangeStatus = KeyExchangeStatus.ONE_WAY.name,
                    locallyImported = true,
                    remoteIdentityPacketReceived = true,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )
            )
        }

    suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(expectedRemoteEncryptionPublicKey.isNotEmpty()) {
                "Expected encryption key must not be empty"
            }

            require(expectedRemoteSigningPublicKey.isNotEmpty()) {
                "Expected signing key must not be empty"
            }

            val updatedRows =
                contactDao.updateKeyExchangeStatusIfKeysMatch(
                    contactId = contactId,
                    expectedEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
                    expectedSigningPublicKey = expectedRemoteSigningPublicKey,
                    keyExchangeStatus = KeyExchangeStatus.MUTUAL.name,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                )

            check(updatedRows == 1) {
                "Contact identity changed before acknowledgement was applied"
            }
        }

    suspend fun resetAllAfterLocalIdentityChange(): Result<Unit> =
        runCatching {
            contactDao.resetAfterLocalIdentityChange(
                currentKeyExchangeStatus = KeyExchangeStatus.MUTUAL.name,
                keyExchangeStatus = KeyExchangeStatus.ONE_WAY.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }
}
