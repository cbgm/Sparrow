package com.cbgm.sparrow.core.crypto.blob

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

class SodiumBlobCipher : BlobCipher {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Result<EncryptedBlob> =
        runCatching {
            require(plaintext.isNotEmpty()) { "Blob plaintext must not be empty" }
            SodiumRuntime.initialize().getOrThrow()

            val key =
                AuthenticatedEncryptionWithAssociatedData
                    .xChaCha20Poly1305IetfKeygen()
            val nonce = LibsodiumRandom.buf(NONCE_BYTES)
            val ciphertext =
                AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfEncrypt(
                    message = plaintext.toUByteArray(),
                    associatedData = associatedData.toUByteArray(),
                    nonce = nonce,
                    key = key
                )

            EncryptedBlob(
                key = key.toByteArray(),
                nonce = nonce.toByteArray(),
                ciphertext = ciphertext.toByteArray()
            )
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun decrypt(
        ciphertext: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray
    ): Result<ByteArray> =
        runCatching {
            require(ciphertext.isNotEmpty()) { "Blob ciphertext must not be empty" }
            require(key.size == KEY_BYTES) { "Blob key must be $KEY_BYTES bytes" }
            require(nonce.size == NONCE_BYTES) { "Blob nonce must be $NONCE_BYTES bytes" }
            SodiumRuntime.initialize().getOrThrow()

            AuthenticatedEncryptionWithAssociatedData
                .xChaCha20Poly1305IetfDecrypt(
                    ciphertextAndTag = ciphertext.toUByteArray(),
                    associatedData = associatedData.toUByteArray(),
                    nonce = nonce.toUByteArray(),
                    key = key.toUByteArray()
                ).toByteArray()
        }

    private companion object {
        const val KEY_BYTES = 32
        const val NONCE_BYTES = 24
    }
}
