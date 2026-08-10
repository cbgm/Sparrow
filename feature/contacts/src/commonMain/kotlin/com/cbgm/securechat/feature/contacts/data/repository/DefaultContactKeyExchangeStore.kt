package com.cbgm.securechat.feature.contacts.data.repository

import com.cbgm.securechat.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.securechat.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.RemoteIdentityUpdate
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityOrigin

class DefaultContactKeyExchangeStore(
    private val contactDao: ContactDao,
    private val mailboxCapabilityLifecycle: MailboxCapabilityLifecycle =
        NoOpMailboxCapabilityLifecycle
) : ContactKeyExchangeStore {
    override suspend fun storeRemoteIdentity(
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
                val pinnedIdentity = requireNotNull(existing)
                val identityIsPinned =
                    pinnedIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name ||
                        pinnedIdentity.verificationStatus == ContactVerificationStatus.VERIFIED.name

                check(!identityIsPinned) {
                    "Stored mutual or verified identity cannot be replaced without an explicit reset"
                }
                mailboxCapabilityLifecycle.revokeForContact(contactId).getOrThrow()
            }

            val nextLocallyImported =
                sameIdentity && existing?.locallyImported == true ||
                    origin == RemoteIdentityOrigin.LOCAL_IMPORT ||
                    origin == RemoteIdentityOrigin.TRUSTED_QR_IMPORT

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
                        existing?.verificationStatus == ContactVerificationStatus.VERIFIED.name -> {
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
                        verifiedByContact = sameIdentity && existing?.verifiedByContact == true,
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

    override suspend fun acceptRemoteIdentity(
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

    override suspend fun acceptRemoteIdentityForHandshake(
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

    override suspend fun markMutual(
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

    override suspend fun resetAllAfterLocalIdentityChange(): Result<Unit> =
        runCatching {
            contactDao.resetAfterLocalIdentityChange(
                currentKeyExchangeStatus = KeyExchangeStatus.MUTUAL.name,
                keyExchangeStatus = KeyExchangeStatus.ONE_WAY.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

    private fun String.toKeyExchangeStatus(): KeyExchangeStatus =
        KeyExchangeStatus.entries.firstOrNull { status ->
            status.name == this
        } ?: KeyExchangeStatus.ONE_WAY
}
