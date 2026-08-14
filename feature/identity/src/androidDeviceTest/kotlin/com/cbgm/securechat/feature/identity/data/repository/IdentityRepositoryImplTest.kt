package com.cbgm.securechat.feature.identity.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.cbgm.securechat.core.crypto.SodiumRuntime
import com.cbgm.securechat.core.crypto.identity.SodiumIdentityKeyGenerator
import com.cbgm.securechat.core.crypto.signature.SodiumDetachedSignatureCrypto
import com.cbgm.securechat.feature.identity.data.datasource.AndroidPrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.datasource.AndroidPublicIdentityStorage
import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android device tests for the complete identity repository flow.
 *
 * Unlike IdentityCryptoTest, this test does not test only
 * cryptographic key generation.
 *
 * It tests several real implementations together:
 *
 * - libsodium key generation
 * - Android Keystore-backed private-key protection
 * - SharedPreferences persistence
 * - repository coordination
 */
class IdentityRepositoryImplTest {
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun identityCanBeCreatedStoredAndLoaded(): Unit =
        runBlocking {
            /**
             * Initialize libsodium once through our central runtime.
             */
            SodiumRuntime.initialize().getOrThrow()

            /**
             * Obtain the Android application Context.
             *
             * Because this is an Android device test, this is a real
             * Android Context from the emulator or physical device.
             */
            val context = ApplicationProvider.getApplicationContext<Context>()

            /**
             * Create the real Android storage implementations.
             */
            val privateKeyStorage = AndroidPrivateKeyStorage(context = context)

            val publicIdentityStorage = AndroidPublicIdentityStorage(context = context)

            /**
             * Start with a clean state.
             *
             * Device-test data can survive between test executions,
             * so tests should never assume storage is empty.
             */
            privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()

            publicIdentityStorage.delete().getOrThrow()

            try {
                /**
                 * Build the real repository.
                 *
                 * No fake crypto.
                 * No fake storage.
                 */
                val repository =
                    IdentityRepositoryImpl(
                        identityKeyGenerator = SodiumIdentityKeyGenerator(),
                        signatureCrypto = SodiumDetachedSignatureCrypto(),
                        privateKeyStorage = privateKeyStorage,
                        publicIdentityStorage = publicIdentityStorage
                    )

                /**
                 * Before creation, no complete identity should exist.
                 */
                val existsBeforeCreation =
                    repository
                        .hasIdentity()
                        .getOrThrow()

                assertFalse(
                    existsBeforeCreation,
                    "Identity should not exist before creation"
                )

                /**
                 * Create a completely new identity.
                 */
                val createdIdentity = repository.createIdentity().getOrThrow()

                /**
                 * Verify that public key material was returned.
                 */
                assertTrue(
                    createdIdentity.encryptionPublicKey.isNotEmpty(),
                    "Created encryption public key must not be empty"
                )

                assertTrue(
                    createdIdentity.signingPublicKey.isNotEmpty(),
                    "Created signing public key must not be empty"
                )

                /**
                 * After creation, both public and private identity
                 * storage should exist.
                 */
                val existsAfterCreation = repository.hasIdentity().getOrThrow()

                assertTrue(
                    existsAfterCreation,
                    "Identity should exist after creation"
                )

                /**
                 * Load the public identity through the repository.
                 */
                val loadedIdentity = repository.getIdentity().getOrThrow()

                assertNotNull(
                    loadedIdentity,
                    "Loaded public identity must not be null"
                )

                /**
                 * The stored public identity must exactly match
                 * the identity returned during creation.
                 */
                assertContentEquals(
                    createdIdentity.encryptionPublicKey,
                    loadedIdentity.encryptionPublicKey,
                    "Loaded encryption public key must match created key"
                )

                assertContentEquals(
                    createdIdentity.signingPublicKey,
                    loadedIdentity.signingPublicKey,
                    "Loaded signing public key must match created key"
                )

                /**
                 * Verify that protected private keys can also be
                 * recovered through the private storage abstraction.
                 *
                 * We do not expose these through IdentityRepository.
                 */
                val loadedEncryptionPrivateKey =
                    privateKeyStorage.loadEncryptionPrivateKey().getOrThrow()

                val loadedSigningPrivateKey = privateKeyStorage.loadSigningPrivateKey().getOrThrow()

                assertNotNull(
                    loadedEncryptionPrivateKey,
                    "Encryption private key must exist after creation"
                )

                assertNotNull(
                    loadedSigningPrivateKey,
                    "Signing private key must exist after creation"
                )
            } finally {
                /**
                 * Always remove test identity data.
                 *
                 * This runs even if an assertion fails.
                 */
                privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()

                publicIdentityStorage.delete().getOrThrow()
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun creatingIdentityTwiceDoesNotReplaceExistingIdentity() =
        runBlocking {
            /**
             * Initialize libsodium through our shared runtime.
             */
            SodiumRuntime.initialize().getOrThrow()

            val context = ApplicationProvider.getApplicationContext<Context>()

            val privateKeyStorage = AndroidPrivateKeyStorage(context = context)

            val publicIdentityStorage = AndroidPublicIdentityStorage(context = context)

            /**
             * Start from a clean state.
             */
            privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()

            publicIdentityStorage.delete().getOrThrow()

            try {
                val repository =
                    IdentityRepositoryImpl(
                        identityKeyGenerator = SodiumIdentityKeyGenerator(),
                        signatureCrypto = SodiumDetachedSignatureCrypto(),
                        privateKeyStorage = privateKeyStorage,
                        publicIdentityStorage = publicIdentityStorage
                    )

                /**
                 * First creation should succeed.
                 */
                val firstIdentity = repository.createIdentity().getOrThrow()

                /**
                 * Save copies of the original private keys.
                 *
                 * We need copies because later we want to verify that
                 * a failed second creation did not replace them.
                 */
                val firstEncryptionPrivateKey =
                    privateKeyStorage
                        .loadEncryptionPrivateKey()
                        .getOrThrow()
                        ?.copyOf()

                val firstSigningPrivateKey =
                    privateKeyStorage
                        .loadSigningPrivateKey()
                        .getOrThrow()
                        ?.copyOf()

                assertNotNull(
                    firstEncryptionPrivateKey,
                    "First encryption private key must exist"
                )

                assertNotNull(
                    firstSigningPrivateKey,
                    "First signing private key must exist"
                )

                /**
                 * Second creation must fail.
                 */
                val secondCreationResult = repository.createIdentity()

                assertTrue(
                    secondCreationResult.isFailure,
                    "Creating an identity twice must fail"
                )

                /**
                 * Load the public identity again.
                 */
                val identityAfterSecondAttempt = repository.getIdentity().getOrThrow()

                assertNotNull(
                    identityAfterSecondAttempt,
                    "Original identity must still exist"
                )

                /**
                 * Verify public keys were not replaced.
                 */
                assertContentEquals(
                    firstIdentity.encryptionPublicKey,
                    identityAfterSecondAttempt.encryptionPublicKey,
                    "Encryption public key must not change"
                )

                assertContentEquals(
                    firstIdentity.signingPublicKey,
                    identityAfterSecondAttempt.signingPublicKey,
                    "Signing public key must not change"
                )

                /**
                 * Load private keys after the failed second attempt.
                 */
                val encryptionPrivateKeyAfterSecondAttempt =
                    privateKeyStorage
                        .loadEncryptionPrivateKey()
                        .getOrThrow()

                val signingPrivateKeyAfterSecondAttempt =
                    privateKeyStorage
                        .loadSigningPrivateKey()
                        .getOrThrow()

                assertNotNull(
                    encryptionPrivateKeyAfterSecondAttempt,
                    "Encryption private key must still exist"
                )

                assertNotNull(
                    signingPrivateKeyAfterSecondAttempt,
                    "Signing private key must still exist"
                )

                /**
                 * Verify private keys were not replaced.
                 */
                assertContentEquals(
                    firstEncryptionPrivateKey,
                    encryptionPrivateKeyAfterSecondAttempt,
                    "Encryption private key must not change"
                )

                assertContentEquals(
                    firstSigningPrivateKey,
                    signingPrivateKeyAfterSecondAttempt,
                    "Signing private key must not change"
                )
            } finally {
                /**
                 * Always clean up test data.
                 */
                privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()

                publicIdentityStorage.delete().getOrThrow()
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun mismatchedSigningPublicAndPrivateKeysAreIncomplete() =
        runBlocking {
            SodiumRuntime.initialize().getOrThrow()

            val context = ApplicationProvider.getApplicationContext<Context>()
            val privateKeyStorage = AndroidPrivateKeyStorage(context = context)
            val publicIdentityStorage = AndroidPublicIdentityStorage(context = context)

            privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()
            publicIdentityStorage.delete().getOrThrow()

            try {
                val keyGenerator = SodiumIdentityKeyGenerator()
                val privateIdentity = keyGenerator.generate().getOrThrow()
                val unrelatedPublicIdentity = keyGenerator.generate().getOrThrow()

                privateKeyStorage
                    .saveIdentityPrivateKeys(
                        encryptionPrivateKey = privateIdentity.encryptionPrivateKey,
                        signingPrivateKey = privateIdentity.signingPrivateKey
                    ).getOrThrow()

                publicIdentityStorage
                    .save(
                        PublicIdentity(
                            encryptionPublicKey = unrelatedPublicIdentity.encryptionPublicKey.toByteArray(),
                            signingPublicKey = unrelatedPublicIdentity.signingPublicKey.toByteArray()
                        )
                    ).getOrThrow()

                val repository =
                    IdentityRepositoryImpl(
                        identityKeyGenerator = keyGenerator,
                        signatureCrypto = SodiumDetachedSignatureCrypto(),
                        privateKeyStorage = privateKeyStorage,
                        publicIdentityStorage = publicIdentityStorage
                    )

                val status = repository.getStatus().getOrThrow()

                assertTrue(
                    status == IdentityStatus.INCOMPLETE,
                    "A public identity that does not match the stored private signing key must be incomplete"
                )
            } finally {
                privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()
                publicIdentityStorage.delete().getOrThrow()
            }
        }
}
