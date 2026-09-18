package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityHandshakeRepository

internal class RemoteIdentityHandshakeRepositoryImpl(
    private val dataSource: ContactKeyExchangeDataSource
) : RemoteIdentityHandshakeRepository {
    override suspend fun stage(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean> =
        safeSuspendCall {
            dataSource
                .storeRemoteIdentity(
                    contactId = contactId,
                    encryptionPublicKey = encryptionPublicKey,
                    signingPublicKey = signingPublicKey,
                    origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                ).identityChanged
        }

    override suspend fun accept(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.prepareRemoteIdentityForHandshake(
                contactId = contactId,
                remoteEncryptionPublicKey = encryptionPublicKey,
                remoteSigningPublicKey = signingPublicKey
            )
        }

    override suspend fun establishMutual(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.prepareRemoteIdentityForHandshake(
                contactId = contactId,
                remoteEncryptionPublicKey = encryptionPublicKey,
                remoteSigningPublicKey = signingPublicKey
            )
            dataSource.markMutual(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                expectedRemoteSigningPublicKey = signingPublicKey
            )
        }

    override suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        safeSuspendCall {
            dataSource.ensureSigningIdentityMatches(
                contactId = contactId,
                signingPublicKey = signingPublicKey
            )
        }
}
