package com.cbgm.sparrow.feature.identity.domain.repository

interface RemoteIdentityHandshakeRepository {
    suspend fun stage(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Boolean>

    suspend fun accept(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit>

    suspend fun establishMutual(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit>

    suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ): Result<Unit>
}
