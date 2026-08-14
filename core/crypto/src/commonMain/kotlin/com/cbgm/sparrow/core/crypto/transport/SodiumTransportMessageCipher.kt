package com.cbgm.sparrow.core.crypto.transport

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.cbgm.sparrow.core.crypto.error.InvalidPrivateKeyException
import com.cbgm.sparrow.core.crypto.error.InvalidPublicKeyException
import com.cbgm.sparrow.core.crypto.error.MessageDecryptionException
import com.cbgm.sparrow.core.crypto.error.MessageEncryptionException
import com.cbgm.sparrow.core.crypto.error.UnsupportedCryptoVersionException
import com.ionspin.kotlin.crypto.box.Box

class SodiumTransportMessageCipher : TransportMessageCipher {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun encryptForRecipient(
        plaintext: ByteArray,
        recipientPublicKey: ByteArray
    ): Result<EncryptedTransportPayload> =
        runCatching {
            require(plaintext.isNotEmpty()) {
                "Plaintext must not be empty"
            }

            validatePublicKey(publicKey = recipientPublicKey)

            SodiumRuntime.initialize().getOrThrow()

            val ciphertext: UByteArray =
                Box.seal(
                    message = plaintext.toUByteArray(),
                    recipientsPublicKey = recipientPublicKey.toUByteArray()
                )

            EncryptedTransportPayload(
                version = CURRENT_VERSION,
                mode = TransportEncryptionMode.SEALED_BOX,
                payload = ciphertext.toByteArray()
            )
        }.recoverCatching { error ->
            when (error) {
                is InvalidPublicKeyException -> {
                    throw error
                }

                else -> {
                    throw MessageEncryptionException(
                        cause = error
                    )
                }
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun decryptFromSender(
        encryptedPayload: EncryptedTransportPayload,
        localPublicKey: ByteArray,
        localPrivateKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            if (encryptedPayload.version != CURRENT_VERSION) {
                throw UnsupportedCryptoVersionException(
                    version =
                        encryptedPayload.version
                )
            }

            require(encryptedPayload.mode == TransportEncryptionMode.SEALED_BOX) {
                "Expected a sealed-box payload"
            }

            validatePublicKey(publicKey = localPublicKey)

            validatePrivateKey(privateKey = localPrivateKey)

            SodiumRuntime.initialize().getOrThrow()

            val plaintext: UByteArray =
                Box.sealOpen(
                    ciphertext = encryptedPayload.payload.toUByteArray(),
                    recipientsPublicKey = localPublicKey.toUByteArray(),
                    recipientsSecretKey = localPrivateKey.toUByteArray()
                )

            plaintext.toByteArray()
        }.recoverCatching { error ->
            when (error) {
                is InvalidPublicKeyException,
                is InvalidPrivateKeyException,
                is UnsupportedCryptoVersionException
                -> {
                    throw error
                }

                else -> {
                    throw MessageDecryptionException(
                        cause = error
                    )
                }
            }
        }

    private fun validatePublicKey(publicKey: ByteArray) {
        if (publicKey.size != BOX_PUBLIC_KEY_SIZE) {
            throw InvalidPublicKeyException(
                message = "Expected a $BOX_PUBLIC_KEY_SIZE-byte public key, but received ${publicKey.size}"
            )
        }
    }

    private fun validatePrivateKey(privateKey: ByteArray) {
        if (privateKey.size != BOX_PRIVATE_KEY_SIZE) {
            throw InvalidPrivateKeyException(
                message = "Expected a $BOX_PRIVATE_KEY_SIZE-byte private key, but received ${privateKey.size}"
            )
        }
    }

    private companion object {
        const val CURRENT_VERSION = 1
        const val BOX_PUBLIC_KEY_SIZE = 32
        const val BOX_PRIVATE_KEY_SIZE = 32
    }
}
