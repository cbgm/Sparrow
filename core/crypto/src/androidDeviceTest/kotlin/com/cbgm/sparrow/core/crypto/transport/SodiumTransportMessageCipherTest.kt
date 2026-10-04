package com.cbgm.sparrow.core.crypto.transport

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.box.Box
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class SodiumTransportMessageCipherTest {
    private val cipher =
        SodiumTransportMessageCipher()

    @Test
    fun encryptedMessageCanBeDecryptedByRecipient() =
        runTest {
            val recipient = generateEncryptionKeyPair()

            val plaintext =
                "Hello secure world"
                    .encodeToByteArray()

            val encrypted =
                cipher
                    .encryptForRecipient(
                        plaintext = plaintext,
                        recipientPublicKey =
                            recipient.publicKey
                    ).getOrThrow()

            assertTrue(
                encrypted.mode ==
                    TransportEncryptionMode
                        .SEALED_BOX
            )

            val decrypted =
                cipher
                    .decryptFromSender(
                        encryptedPayload =
                        encrypted,
                        localPublicKey =
                            recipient.publicKey,
                        localPrivateKey =
                            recipient.secretKey
                    ).getOrThrow()

            assertContentEquals(
                expected = plaintext,
                actual = decrypted
            )
        }

    @Test
    fun anotherIdentityCannotDecryptMessage() =
        runTest {
            val recipient = generateEncryptionKeyPair()

            val attacker = generateEncryptionKeyPair()

            val encrypted =
                cipher
                    .encryptForRecipient(
                        plaintext =
                            "Private message"
                                .encodeToByteArray(),
                        recipientPublicKey =
                            recipient.publicKey
                    ).getOrThrow()

            val result =
                cipher.decryptFromSender(
                    encryptedPayload =
                    encrypted,
                    localPublicKey =
                        attacker.publicKey,
                    localPrivateKey =
                        attacker.secretKey
                )

            assertTrue(
                result.isFailure
            )
        }

    private suspend fun generateEncryptionKeyPair(): TestEncryptionKeyPair {
        SodiumRuntime.initialize().getOrThrow()
        val keyPair = Box.keypair()
        return TestEncryptionKeyPair(
            publicKey = keyPair.publicKey.toByteArray(),
            secretKey = keyPair.secretKey.toByteArray()
        )
    }

    private class TestEncryptionKeyPair(
        val publicKey: ByteArray,
        val secretKey: ByteArray
    )
}
