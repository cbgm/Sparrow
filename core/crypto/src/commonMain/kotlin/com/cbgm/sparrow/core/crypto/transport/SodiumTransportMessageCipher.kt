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
                    message = TransportPadding.pad(plaintext).toUByteArray(),
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
            if (encryptedPayload.version !in READABLE_VERSIONS) {
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

            // Version 1 envelopes, sealed before padding, may still wait in mailboxes.
            if (encryptedPayload.version == UNPADDED_VERSION) plaintext.toByteArray() else TransportPadding.unpad(plaintext.toByteArray())
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
        /** Version 2: the packet is padded to a size class before it is sealed ([TransportPadding]). */
        const val CURRENT_VERSION = 2
        const val UNPADDED_VERSION = 1
        val READABLE_VERSIONS = setOf(UNPADDED_VERSION, CURRENT_VERSION)
        const val BOX_PUBLIC_KEY_SIZE = 32
        const val BOX_PRIVATE_KEY_SIZE = 32
    }
}
