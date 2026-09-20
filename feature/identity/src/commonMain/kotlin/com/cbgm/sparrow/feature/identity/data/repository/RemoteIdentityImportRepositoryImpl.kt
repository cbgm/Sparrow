package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.protocol.mailbox.MailboxCapabilityLifecycle
import com.cbgm.sparrow.core.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.dao.RemoteIdentityDao
import com.cbgm.sparrow.feature.identity.data.datasource.RemoteIdentityDataSource
import com.cbgm.sparrow.feature.identity.data.model.RemoteIdentityImportOriginDto
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityUpdate
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityImportRepository

/** Identity owns persistence and trust transitions; Contacts only persists address-book metadata. */
class RemoteIdentityImportRepositoryImpl(
    remoteIdentityDao: RemoteIdentityDao,
    mailboxCapabilityLifecycle: MailboxCapabilityLifecycle = NoOpMailboxCapabilityLifecycle
) : RemoteIdentityImportRepository {
    private val source = RemoteIdentityDataSource(remoteIdentityDao, mailboxCapabilityLifecycle)

    override suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate> = safeSuspendCall {
        val (stored, identityChanged) = source.storeImported(
            peerId = contactId,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey,
            origin = RemoteIdentityImportOriginDto.valueOf(origin.name)
        )
        RemoteIdentityUpdate(
            contactId = contactId,
            encryptionPublicKey = stored.encryptionPublicKey.copyOf(),
            signingPublicKey = stored.signingPublicKey.copyOf(),
            keyExchangeStatus = KeyExchangeStatus.valueOf(stored.keyExchangeStatus),
            verificationStatus = ContactVerificationStatus.valueOf(stored.verificationStatus),
            identityChanged = identityChanged
        )
    }

    override suspend fun acceptRemoteIdentity(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> = safeSuspendCall {
        source.acceptImported(contactId, expectedRemoteEncryptionPublicKey, expectedRemoteSigningPublicKey)
    }

    override suspend fun acceptRemoteIdentityForHandshake(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> = safeSuspendCall {
        source.acceptStoredHandshake(contactId, expectedRemoteEncryptionPublicKey, expectedRemoteSigningPublicKey)
    }

    override suspend fun prepareRemoteIdentityForHandshake(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): Result<Unit> = safeSuspendCall {
        source.accept(contactId, remoteEncryptionPublicKey, remoteSigningPublicKey)
    }

    override suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> = safeSuspendCall {
        source.markMutual(contactId, expectedRemoteEncryptionPublicKey, expectedRemoteSigningPublicKey)
    }
}
