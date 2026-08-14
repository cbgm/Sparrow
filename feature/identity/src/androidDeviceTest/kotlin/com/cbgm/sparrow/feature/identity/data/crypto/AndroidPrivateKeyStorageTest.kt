package com.cbgm.sparrow.feature.identity.data.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.cbgm.sparrow.core.crypto.identity.SodiumIdentityKeyGenerator
import com.cbgm.sparrow.feature.identity.data.datasource.AndroidPrivateKeyStorage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AndroidPrivateKeyStorageTest {
    /**
     * Tests the complete private-key storage round trip:
     *
     * 1. Generate real libsodium identity keys.
     * 2. Encrypt and save private keys.
     * 3. Load and decrypt private keys.
     * 4. Compare loaded bytes with original bytes.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun privateKeysCanBeSavedAndLoaded(): Unit =
        runBlocking {
            SodiumRuntime.initialize().getOrThrow()

            val context = ApplicationProvider.getApplicationContext<Context>()

            val storage = AndroidPrivateKeyStorage(context = context)

            /**
             * Start from a clean test state.
             *
             * This removes encrypted private-key blobs left by
             * an earlier test execution.
             */
            storage.deleteIdentityPrivateKeys().getOrThrow()

            try {
                val identityKeyGenerator = SodiumIdentityKeyGenerator()

                /**
                 * Generate real private keys.
                 */
                val originalKeyPair = identityKeyGenerator.generate().getOrThrow()

                /**
                 * Save both private keys.
                 *
                 * AndroidPrivateKeyStorage will:
                 *
                 * - convert them to ByteArray
                 * - encrypt each one with AES-GCM
                 * - use the Android Keystore wrapping key
                 * - persist ciphertext + IV
                 */
                val saveResult =
                    storage.saveIdentityPrivateKeys(
                        encryptionPrivateKey = originalKeyPair.encryptionPrivateKey,
                        signingPrivateKey = originalKeyPair.signingPrivateKey
                    )

                assertTrue(
                    saveResult.isSuccess,
                    "Saving private keys failed: " + saveResult.exceptionOrNull()?.message
                )

                /**
                 * Load and decrypt the X25519-side private key.
                 */
                val loadedEncryptionPrivateKey = storage.loadEncryptionPrivateKey().getOrThrow()

                /**
                 * Load and decrypt the Ed25519 signing private key.
                 */
                val loadedSigningPrivateKey = storage.loadSigningPrivateKey().getOrThrow()

                assertNotNull(
                    loadedEncryptionPrivateKey,
                    "Loaded encryption private key must not be null"
                )

                assertNotNull(
                    loadedSigningPrivateKey,
                    "Loaded signing private key must not be null"
                )

                /**
                 * Verify exact byte-for-byte equality.
                 *
                 * If these assertions pass, encryption and decryption
                 * preserved the original private-key material.
                 */
                assertContentEquals(
                    originalKeyPair.encryptionPrivateKey,
                    loadedEncryptionPrivateKey
                )

                assertContentEquals(
                    originalKeyPair.signingPrivateKey,
                    loadedSigningPrivateKey
                )
            } finally {
                /**
                 * Always clean up encrypted test data.
                 *
                 * finally runs even if an assertion fails.
                 */
                storage.deleteIdentityPrivateKeys().getOrThrow()
            }
        }
}
