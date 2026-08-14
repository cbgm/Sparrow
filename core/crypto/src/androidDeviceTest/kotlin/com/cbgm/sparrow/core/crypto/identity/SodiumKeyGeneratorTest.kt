package com.cbgm.sparrow.core.crypto.identity

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class SodiumIdentityKeyGeneratorTest {
    private val generator =
        SodiumIdentityKeyGenerator()

    @BeforeTest
    fun initializeSodium() =
        runTest {
            SodiumRuntime
                .initialize()
                .getOrThrow()
        }

    @Test
    fun generateCreatesAllKeys() =
        runTest {
            val result =
                generator.generate()

            assertTrue(
                result.isSuccess
            )

            val keyPair =
                result.getOrThrow()

            assertTrue(
                keyPair
                    .encryptionPublicKey
                    .isNotEmpty()
            )

            assertTrue(
                keyPair
                    .encryptionPrivateKey
                    .isNotEmpty()
            )

            assertTrue(
                keyPair
                    .signingPublicKey
                    .isNotEmpty()
            )

            assertTrue(
                keyPair
                    .signingPrivateKey
                    .isNotEmpty()
            )
        }

    @Test
    fun generateCreatesDifferentIdentities() =
        runTest {
            val first =
                generator
                    .generate()
                    .getOrThrow()

            val second =
                generator
                    .generate()
                    .getOrThrow()

            assertFalse(
                first
                    .encryptionPublicKey
                    .contentEquals(
                        second
                            .encryptionPublicKey
                    )
            )

            assertFalse(
                first
                    .signingPublicKey
                    .contentEquals(
                        second
                            .signingPublicKey
                    )
            )
        }
}
