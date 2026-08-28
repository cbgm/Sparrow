package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.feature.contacts.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.contacts.domain.model.RemoteIdentityUpdate
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository

class ContactKeyExchangeRepositoryImpl(
    private val dataSource: ContactKeyExchangeDataSource
) : ContactKeyExchangeRepository {
    override suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate> =
        dataSource.storeRemoteIdentity(
            contactId = contactId,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey,
            origin = origin
        )

    override suspend fun acceptRemoteIdentity(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        dataSource.acceptRemoteIdentity(
            contactId = contactId,
            expectedRemoteEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
            expectedRemoteSigningPublicKey = expectedRemoteSigningPublicKey
        )

    override suspend fun acceptRemoteIdentityForHandshake(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        dataSource.acceptRemoteIdentityForHandshake(
            contactId = contactId,
            expectedRemoteEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
            expectedRemoteSigningPublicKey = expectedRemoteSigningPublicKey
        )

    override suspend fun acceptInvitationIdentityForHandshake(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        dataSource.acceptInvitationIdentityForHandshake(
            contactId = contactId,
            remoteEncryptionPublicKey = remoteEncryptionPublicKey,
            remoteSigningPublicKey = remoteSigningPublicKey
        )

    override suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        dataSource.markMutual(
            contactId = contactId,
            expectedRemoteEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
            expectedRemoteSigningPublicKey = expectedRemoteSigningPublicKey
        )

    override suspend fun resetAllAfterLocalIdentityChange(): Result<Unit> =
        dataSource.resetAllAfterLocalIdentityChange()
}
