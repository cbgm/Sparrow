package com.cbgm.sparrow.core.crypto.safety

import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.core.crypto.model.PublicIdentityKeySet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SafetyNumberGeneratorTest {
    private val generator =
        SafetyNumberGenerator(
            cryptoHash =
                DefaultCryptoHash()
        )

    @Test
    fun participantOrderDoesNotChangeNumber() {
        val alice =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        5,
                        6,
                        7,
                        8
                    )
            )

        val bob =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(
                        11,
                        12,
                        13,
                        14
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        15,
                        16,
                        17,
                        18
                    )
            )

        val aliceView =
            generator
                .generate(
                    firstIdentity = alice,
                    secondIdentity = bob
                ).getOrThrow()

        val bobView =
            generator
                .generate(
                    firstIdentity = bob,
                    secondIdentity = alice
                ).getOrThrow()

        assertEquals(
            expected =
                aliceView.groups,
            actual =
                bobView.groups
        )
    }

    @Test
    fun changedEncryptionKeyChangesNumber() {
        val local =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        4,
                        5,
                        6
                    )
            )

        val originalRemote =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(
                        7,
                        8,
                        9
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        10,
                        11,
                        12
                    )
            )

        val changedRemote =
            PublicIdentityKeySet(
                signingPublicKey =
                    originalRemote
                        .signingPublicKey,
                encryptionPublicKey =
                    byteArrayOf(
                        10,
                        11,
                        13
                    )
            )

        val originalNumber =
            generator
                .generate(
                    firstIdentity =
                    local,
                    secondIdentity =
                    originalRemote
                ).getOrThrow()

        val changedNumber =
            generator
                .generate(
                    firstIdentity =
                    local,
                    secondIdentity =
                    changedRemote
                ).getOrThrow()

        assertNotEquals(
            illegal =
                originalNumber.groups,
            actual =
                changedNumber.groups
        )
    }

    @Test
    fun changedSigningKeyChangesNumber() {
        val local =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(1),
                encryptionPublicKey =
                    byteArrayOf(2)
            )

        val originalRemote =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(3),
                encryptionPublicKey =
                    byteArrayOf(4)
            )

        val changedRemote =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(5),
                encryptionPublicKey =
                    byteArrayOf(4)
            )

        val originalNumber =
            generator
                .generate(
                    firstIdentity =
                    local,
                    secondIdentity =
                    originalRemote
                ).getOrThrow()

        val changedNumber =
            generator
                .generate(
                    firstIdentity =
                    local,
                    secondIdentity =
                    changedRemote
                ).getOrThrow()

        assertNotEquals(
            illegal =
                originalNumber.groups,
            actual =
                changedNumber.groups
        )
    }

    @Test
    fun outputContainsFullSha256Digest() {
        val first =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(1),
                encryptionPublicKey =
                    byteArrayOf(2)
            )

        val second =
            PublicIdentityKeySet(
                signingPublicKey =
                    byteArrayOf(3),
                encryptionPublicKey =
                    byteArrayOf(4)
            )

        val safetyNumber =
            generator
                .generate(
                    firstIdentity = first,
                    secondIdentity = second
                ).getOrThrow()

        assertEquals(
            expected = 16,
            actual =
                safetyNumber.groups.size
        )

        safetyNumber.groups
            .forEach { group ->
                assertEquals(
                    expected = 5,
                    actual =
                        group.length
                )
            }
    }
}
