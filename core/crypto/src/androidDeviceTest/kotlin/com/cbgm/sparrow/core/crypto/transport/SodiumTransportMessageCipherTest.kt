package com.cbgm.sparrow.core.crypto.transport

import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityKeyGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class SodiumTransportMessageCipherTest {
    private val identityKeyGenerator =
        SodiumIdentityKeyGenerator()

    private val cipher =
        SodiumTransportMessageCipher()

    @Test
    fun encryptedMessageCanBeDecryptedByRecipient() =
        runTest {
            val recipient =
                identityKeyGenerator
                    .generate()
                    .getOrThrow()

            val plaintext =
                "Hello secure world"
                    .encodeToByteArray()

            val encrypted =
                cipher
                    .encryptForRecipient(
                        plaintext = plaintext,
                        recipientPublicKey =
                            recipient
                                .encryptionPublicKey
                                .toByteArray()
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
                            recipient
                                .encryptionPublicKey
                                .toByteArray(),
                        localPrivateKey =
                            recipient
                                .encryptionPrivateKey
                                .toByteArray()
                    ).getOrThrow()

            assertContentEquals(
                expected = plaintext,
                actual = decrypted
            )
        }

    @Test
    fun anotherIdentityCannotDecryptMessage() =
        runTest {
            val recipient =
                identityKeyGenerator
                    .generate()
                    .getOrThrow()

            val attacker =
                identityKeyGenerator
                    .generate()
                    .getOrThrow()

            val encrypted =
                cipher
                    .encryptForRecipient(
                        plaintext =
                            "Private message"
                                .encodeToByteArray(),
                        recipientPublicKey =
                            recipient
                                .encryptionPublicKey
                                .toByteArray()
                    ).getOrThrow()

            val result =
                cipher.decryptFromSender(
                    encryptedPayload =
                    encrypted,
                    localPublicKey =
                        attacker
                            .encryptionPublicKey
                            .toByteArray(),
                    localPrivateKey =
                        attacker
                            .encryptionPrivateKey
                            .toByteArray()
                )

            assertTrue(
                result.isFailure
            )
        }
}
