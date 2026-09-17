package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactKeyExchangeRepository
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityUpdate

class ContactKeyExchangeRepositoryImpl(
    private val dataSource: ContactKeyExchangeDataSource
) : ContactKeyExchangeRepository {
    override suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate> =
        safeSuspendCall {
            dataSource.storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                origin = origin
            )
        }

    override suspend fun acceptRemoteIdentity(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.acceptRemoteIdentity(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
                expectedRemoteSigningPublicKey = expectedRemoteSigningPublicKey
            )
        }

    override suspend fun acceptRemoteIdentityForHandshake(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.acceptRemoteIdentityForHandshake(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
                expectedRemoteSigningPublicKey = expectedRemoteSigningPublicKey
            )
        }

    override suspend fun prepareRemoteIdentityForHandshake(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.prepareRemoteIdentityForHandshake(
                contactId = contactId,
                remoteEncryptionPublicKey = remoteEncryptionPublicKey,
                remoteSigningPublicKey = remoteSigningPublicKey
            )
        }

    override suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.markMutual(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = expectedRemoteEncryptionPublicKey,
                expectedRemoteSigningPublicKey = expectedRemoteSigningPublicKey
            )
        }

    override suspend fun resetAllAfterLocalIdentityChange(): Result<Unit> =
        safeSuspendCall { dataSource.resetAllAfterLocalIdentityChange() }
}
