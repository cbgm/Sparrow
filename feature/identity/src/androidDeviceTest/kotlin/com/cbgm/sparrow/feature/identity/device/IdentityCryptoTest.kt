package com.cbgm.sparrow.feature.identity.device

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityKeyGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class IdentityCryptoTest {
    private val generator = SodiumIdentityKeyGenerator()

    @BeforeTest
    fun initializeSodium() =
        runTest {
            SodiumRuntime.initialize().getOrThrow()
        }

    @Test
    fun generatedIdentityContainsRealKeyMaterial() =
        runTest {
            val keyPair = generator.generate().getOrThrow()

            assertTrue(keyPair.encryptionPublicKey.isNotEmpty())
            assertTrue(keyPair.encryptionPrivateKey.isNotEmpty())
            assertTrue(keyPair.signingPublicKey.isNotEmpty())
            assertTrue(keyPair.signingPrivateKey.isNotEmpty())
            assertFalse(keyPair.encryptionPublicKey.all { it == 0.toUByte() })
            assertFalse(keyPair.signingPublicKey.all { it == 0.toUByte() })
        }

    @Test
    fun separatelyGeneratedIdentitiesAreDifferent() =
        runTest {
            val first = generator.generate().getOrThrow()
            val second = generator.generate().getOrThrow()

            assertFalse(first.encryptionPublicKey.contentEquals(second.encryptionPublicKey))
            assertFalse(first.signingPublicKey.contentEquals(second.signingPublicKey))
        }
}
