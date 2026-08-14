package com.cbgm.sparrow.core.crypto.identity

interface IdentityAcknowledgementCrypto {
    suspend fun sign(
        acknowledgedEncryptionPublicKey: ByteArray,
        acknowledgedSigningPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray,
        senderSigningPrivateKey: ByteArray
    ): Result<ByteArray>

    suspend fun verify(
        acknowledgedEncryptionPublicKey: ByteArray,
        acknowledgedSigningPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray,
        signature: ByteArray
    ): Result<Unit>
}
