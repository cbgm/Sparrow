package com.cbgm.sparrow.core.crypto.group

import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityKeyGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class SodiumGroupCryptoTest {
    @Test
    fun invitationChallengeUsesIndependentRandomBytes() =
        runTest {
            val groupCrypto = SodiumGroupCrypto()
            val first = groupCrypto.generateInvitationChallenge().getOrThrow()
            val second = groupCrypto.generateInvitationChallenge().getOrThrow()

            assertEquals(32, first.size)
            assertEquals(32, second.size)
            assertFalse(first.contentEquals(second))
        }

    @Test
    fun wrapsSignsEncryptsAndDecryptsGroupMaterial() =
        runTest {
            val groupCrypto = SodiumGroupCrypto()
            val identity = SodiumIdentityKeyGenerator().generate().getOrThrow()
            val groupKey = groupCrypto.generateGroupKey().getOrThrow()
            val wrappedKey =
                groupCrypto
                    .wrapGroupKey(
                        groupKey = groupKey,
                        recipientEncryptionPublicKey = identity.encryptionPublicKey.toByteArray()
                    ).getOrThrow()
            val unwrappedKey =
                groupCrypto
                    .unwrapGroupKey(
                        wrappedGroupKey = wrappedKey,
                        localEncryptionPublicKey = identity.encryptionPublicKey.toByteArray(),
                        localEncryptionPrivateKey = identity.encryptionPrivateKey.toByteArray()
                    ).getOrThrow()

            assertContentEquals(groupKey, unwrappedKey)

            val associatedData = "group-1:epoch-1:message-1".encodeToByteArray()
            val plaintext = "hello secure group".encodeToByteArray()
            val encrypted =
                groupCrypto
                    .encryptMessage(
                        plaintext = plaintext,
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
            val signaturePayload = associatedData + encrypted.nonce + encrypted.ciphertext
            val signature =
                groupCrypto
                    .sign(
                        payload = signaturePayload,
                        signingPrivateKey = identity.signingPrivateKey.toByteArray()
                    ).getOrThrow()

            groupCrypto
                .verify(
                    payload = signaturePayload,
                    signature = signature,
                    signingPublicKey = identity.signingPublicKey.toByteArray()
                ).getOrThrow()

            val decrypted =
                groupCrypto
                    .decryptMessage(
                        ciphertext = encrypted,
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()

            assertContentEquals(plaintext, decrypted)
        }

    @Test
    fun rejectsTamperedGroupCiphertextAndSignature() =
        runTest {
            val groupCrypto = SodiumGroupCrypto()
            val identity = SodiumIdentityKeyGenerator().generate().getOrThrow()
            val groupKey = groupCrypto.generateGroupKey().getOrThrow()
            val associatedData = "authenticated header".encodeToByteArray()
            val encrypted =
                groupCrypto
                    .encryptMessage(
                        plaintext = "secret".encodeToByteArray(),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
            val signature =
                groupCrypto
                    .sign(
                        payload = encrypted.ciphertext,
                        signingPrivateKey = identity.signingPrivateKey.toByteArray()
                    ).getOrThrow()
            val tamperedCiphertext =
                encrypted.copy(
                    ciphertext =
                        encrypted.ciphertext.copyOf().also { bytes ->
                            bytes[0] = (bytes[0].toInt() xor 1).toByte()
                        }
                )

            assertTrue(
                groupCrypto
                    .decryptMessage(
                        ciphertext = tamperedCiphertext,
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).isFailure
            )
            assertTrue(
                groupCrypto
                    .verify(
                        payload = tamperedCiphertext.ciphertext,
                        signature = signature,
                        signingPublicKey = identity.signingPublicKey.toByteArray()
                    ).isFailure
            )
        }
}
