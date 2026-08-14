package com.cbgm.sparrow.core.crypto.transport

interface TransportMessageCipher {
    /**
     * Encrypts a message for the owner of recipientPublicKey.
     *
     * The resulting packet contains no plaintext.
     */
    suspend fun encryptForRecipient(
        plaintext: ByteArray,
        recipientPublicKey: ByteArray
    ): Result<EncryptedTransportPayload>

    /**
     * Decrypts an encrypted packet using the local encryption key pair.
     */
    suspend fun decryptFromSender(
        encryptedPayload: EncryptedTransportPayload,
        localPublicKey: ByteArray,
        localPrivateKey: ByteArray
    ): Result<ByteArray>
}
