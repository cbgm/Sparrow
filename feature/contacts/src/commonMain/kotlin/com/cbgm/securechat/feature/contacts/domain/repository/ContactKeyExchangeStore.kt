package com.cbgm.securechat.feature.contacts.domain.repository

import com.cbgm.securechat.feature.contacts.domain.model.RemoteIdentityUpdate

enum class RemoteIdentityOrigin {
    LOCAL_IMPORT,
    TRUSTED_QR_IMPORT,
    REMOTE_PACKET,
    CONTACT_INVITATION
}

interface ContactKeyExchangeStore {
    suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray,
        origin: RemoteIdentityOrigin
    ): Result<RemoteIdentityUpdate>

    suspend fun acceptRemoteIdentity(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun acceptRemoteIdentityForHandshake(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun acceptInvitationIdentityForHandshake(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey: ByteArray,
        expectedRemoteSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun resetAllAfterLocalIdentityChange(): Result<Unit>
}
