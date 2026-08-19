package com.cbgm.sparrow.core.crypto.group

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.cbgm.sparrow.core.crypto.error.SignatureVerificationException
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.signature.Signature
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

class SodiumGroupCrypto : GroupCrypto {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun generateGroupKey(): Result<ByteArray> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()

            AuthenticatedEncryptionWithAssociatedData
                .xChaCha20Poly1305IetfKeygen()
                .toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun generateInvitationChallenge(): Result<ByteArray> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()
            LibsodiumRandom.buf(INVITATION_CHALLENGE_SIZE).toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun wrapGroupKey(
        groupKey: ByteArray,
        recipientEncryptionPublicKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            requireGroupKey(groupKey)
            requireEncryptionPublicKey(recipientEncryptionPublicKey)
            SodiumRuntime.initialize().getOrThrow()

            Box
                .seal(
                    message = groupKey.toUByteArray(),
                    recipientsPublicKey = recipientEncryptionPublicKey.toUByteArray()
                ).toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun unwrapGroupKey(
        wrappedGroupKey: ByteArray,
        localEncryptionPublicKey: ByteArray,
        localEncryptionPrivateKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            require(wrappedGroupKey.isNotEmpty()) { "Wrapped group key must not be empty" }
            requireEncryptionPublicKey(localEncryptionPublicKey)
            require(localEncryptionPrivateKey.size == ENCRYPTION_PRIVATE_KEY_SIZE) {
                "Local encryption private key must be $ENCRYPTION_PRIVATE_KEY_SIZE bytes"
            }
            SodiumRuntime.initialize().getOrThrow()

            Box
                .sealOpen(
                    ciphertext = wrappedGroupKey.toUByteArray(),
                    recipientsPublicKey = localEncryptionPublicKey.toUByteArray(),
                    recipientsSecretKey = localEncryptionPrivateKey.toUByteArray()
                ).toByteArray()
                .also(::requireGroupKey)
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun encryptMessage(
        plaintext: ByteArray,
        associatedData: ByteArray,
        groupKey: ByteArray
    ): Result<GroupCiphertext> =
        runCatching {
            require(plaintext.isNotEmpty()) { "Group-message plaintext must not be empty" }
            requireGroupKey(groupKey)
            SodiumRuntime.initialize().getOrThrow()

            val nonce = LibsodiumRandom.buf(GROUP_MESSAGE_NONCE_SIZE)
            val ciphertext =
                AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfEncrypt(
                    message = plaintext.toUByteArray(),
                    associatedData = associatedData.toUByteArray(),
                    nonce = nonce,
                    key = groupKey.toUByteArray()
                )

            GroupCiphertext(
                nonce = nonce.toByteArray(),
                ciphertext = ciphertext.toByteArray()
            )
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun decryptMessage(
        ciphertext: GroupCiphertext,
        associatedData: ByteArray,
        groupKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            require(ciphertext.nonce.size == GROUP_MESSAGE_NONCE_SIZE) {
                "Group-message nonce must be $GROUP_MESSAGE_NONCE_SIZE bytes"
            }
            requireGroupKey(groupKey)
            SodiumRuntime.initialize().getOrThrow()

            AuthenticatedEncryptionWithAssociatedData
                .xChaCha20Poly1305IetfDecrypt(
                    ciphertextAndTag = ciphertext.ciphertext.toUByteArray(),
                    associatedData = associatedData.toUByteArray(),
                    nonce = ciphertext.nonce.toUByteArray(),
                    key = groupKey.toUByteArray()
                ).toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun sign(
        payload: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            require(payload.isNotEmpty()) { "Signed group payload must not be empty" }
            require(signingPrivateKey.isNotEmpty()) { "Signing private key must not be empty" }
            SodiumRuntime.initialize().getOrThrow()

            Signature
                .detached(
                    message = payload.toUByteArray(),
                    secretKey = signingPrivateKey.toUByteArray()
                ).toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun verify(
        payload: ByteArray,
        signature: ByteArray,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            require(payload.isNotEmpty()) { "Signed group payload must not be empty" }
            require(signature.isNotEmpty()) { "Group signature must not be empty" }
            require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
            SodiumRuntime.initialize().getOrThrow()

            try {
                Signature.verifyDetached(
                    signature = signature.toUByteArray(),
                    message = payload.toUByteArray(),
                    publicKey = signingPublicKey.toUByteArray()
                )
            } catch (error: Throwable) {
                throw SignatureVerificationException(error)
            }
        }

    private fun requireGroupKey(groupKey: ByteArray) {
        require(groupKey.size == GROUP_KEY_SIZE) {
            "Group key must be $GROUP_KEY_SIZE bytes"
        }
    }

    private fun requireEncryptionPublicKey(publicKey: ByteArray) {
        require(publicKey.size == ENCRYPTION_PUBLIC_KEY_SIZE) {
            "Recipient encryption public key must be $ENCRYPTION_PUBLIC_KEY_SIZE bytes"
        }
    }

    private companion object {
        const val GROUP_KEY_SIZE = 32
        const val INVITATION_CHALLENGE_SIZE = 32
        const val GROUP_MESSAGE_NONCE_SIZE = 24
        const val ENCRYPTION_PUBLIC_KEY_SIZE = 32
        const val ENCRYPTION_PRIVATE_KEY_SIZE = 32
    }
}
