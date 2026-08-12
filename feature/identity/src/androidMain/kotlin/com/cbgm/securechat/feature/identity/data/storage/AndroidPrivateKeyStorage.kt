package com.cbgm.securechat.feature.identity.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.cbgm.securechat.feature.identity.domain.repository.storage.PrivateKeyStorage
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android implementation of [com.cbgm.securechat.core.identity.PrivateKeyStorage].
 *
 * Important:
 * The X25519 and Ed25519 private keys are NOT stored directly
 * inside SharedPreferences.
 *
 * Instead:
 *
 * 1. Android Keystore contains an AES-256 wrapping key.
 * 2. That AES key encrypts our libsodium private-key bytes.
 * 3. Only ciphertext + IV are stored in SharedPreferences.
 *
 * Conceptually:
 *
 * libsodium private key
 *          |
 *          v
 *      AES-GCM
 *          |
 *          v
 * encrypted private key
 *          |
 *          v
 * SharedPreferences
 *
 *
 * The AES wrapping key itself stays in:
 *
 * Android Keystore
 *
 * This is necessary because our minimum SDK is 26 and we cannot
 * assume that X25519 and Ed25519 private keys can themselves be
 * stored directly in Android Keystore on every supported device.
 */
class AndroidPrivateKeyStorage(
    context: Context
) : PrivateKeyStorage {
    /**
     * App-private SharedPreferences storage.
     *
     * We store ONLY:
     *
     * - encrypted private-key ciphertext
     * - AES-GCM IVs
     *
     * We never store plaintext private keys here.
     */
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Access to Android's special cryptographic keystore.
     *
     * Our AES wrapping key will live here.
     *
     * The key is referenced by an alias:
     *
     * securechat_identity_wrapping_key
     */
    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

    /**
     * Encrypts and stores both identity private keys.
     *
     * Input:
     *
     * - X25519 private key
     * - Ed25519 private key
     *
     * Neither key is stored directly.
     *
     * Each key is independently encrypted using AES-GCM.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun saveIdentityPrivateKeys(
        encryptionPrivateKey: UByteArray,
        signingPrivateKey: UByteArray
    ): Result<Unit> =
        runCatching {
            /**
             * Get the AES wrapping key from Android Keystore.
             *
             * If it does not exist yet, create it.
             */
            val wrappingKey = getOrCreateWrappingKey()

            /**
             * Encrypt the X25519 private key.
             *
             * UByteArray is converted to ByteArray because
             * Java/Android Cipher APIs operate on ByteArray.
             */
            val encryptedEncryptionKey =
                encrypt(
                    plainData = encryptionPrivateKey.toByteArray(),
                    wrappingKey = wrappingKey
                )

            /**
             * Encrypt the Ed25519 private key separately.
             *
             * This results in a separate AES-GCM encryption
             * operation with its own IV.
             */
            val encryptedSigningKey =
                encrypt(
                    plainData = signingPrivateKey.toByteArray(),
                    wrappingKey = wrappingKey
                )

            /**
             * Store only encrypted data.
             *
             * We save:
             *
             * X25519 ciphertext
             * X25519 IV
             *
             * Ed25519 ciphertext
             * Ed25519 IV
             *
             * Binary data is Base64 encoded because
             * SharedPreferences stores strings.
             */
            val saved =
                preferences
                    .edit()
                    .putString(
                        ENCRYPTION_PRIVATE_KEY_CIPHERTEXT,
                        encryptedEncryptionKey.cipherText.toBase64()
                    ).putString(
                        ENCRYPTION_PRIVATE_KEY_IV,
                        encryptedEncryptionKey.iv.toBase64()
                    ).putString(
                        SIGNING_PRIVATE_KEY_CIPHERTEXT,
                        encryptedSigningKey.cipherText.toBase64()
                    ).putString(
                        SIGNING_PRIVATE_KEY_IV,
                        encryptedSigningKey.iv.toBase64()
                    )
                    /**
                     * commit() is synchronous.
                     *
                     * We intentionally use commit() instead of apply()
                     * because identity creation should know whether
                     * persistence actually succeeded.
                     */
                    .commit()

            /**
             * If SharedPreferences failed to persist the encrypted
             * data, throw an exception.
             *
             * runCatching converts this into Result.failure().
             */
            check(saved) {
                "Failed to persist encrypted identity private keys"
            }
        }

    /**
     * Checks whether both encrypted private keys appear to exist.
     *
     * A complete identity requires:
     *
     * - X25519 ciphertext
     * - X25519 IV
     * - Ed25519 ciphertext
     * - Ed25519 IV
     *
     * If any part is missing, we return false.
     */
    override suspend fun hasIdentityPrivateKeys(): Result<Boolean> =
        runCatching {
            val encryptedStateExists =
                preferences.contains(ENCRYPTION_PRIVATE_KEY_CIPHERTEXT) &&
                    preferences.contains(ENCRYPTION_PRIVATE_KEY_IV) &&
                    preferences.contains(SIGNING_PRIVATE_KEY_CIPHERTEXT) &&
                    preferences.contains(SIGNING_PRIVATE_KEY_IV)

            if (!encryptedStateExists || getExistingWrappingKey() == null) {
                return@runCatching false
            }

            val encryptionPrivateKey = loadEncryptionPrivateKey().getOrNull()
            val signingPrivateKey = loadSigningPrivateKey().getOrNull()

            encryptionPrivateKey != null && signingPrivateKey != null
        }

    /**
     * Loads and decrypts the X25519 private key.
     *
     * Returns:
     *
     * Result.success(UByteArray)
     *
     * if the key exists and decrypts correctly.
     *
     * Returns:
     *
     * Result.success(null)
     *
     * if the stored key does not exist.
     *
     * Returns:
     *
     * Result.failure(...)
     *
     * if decryption fails.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun loadEncryptionPrivateKey(): Result<UByteArray?> {
        return runCatching {
            /**
             * Load encrypted X25519 key ciphertext.
             *
             * If missing, there is no stored key.
             */
            val cipherTextBase64 =
                preferences.getString(
                    ENCRYPTION_PRIVATE_KEY_CIPHERTEXT,
                    null
                ) ?: return@runCatching null

            /**
             * Load the IV that was generated when the
             * X25519 private key was encrypted.
             */
            val ivBase64 =
                preferences.getString(
                    ENCRYPTION_PRIVATE_KEY_IV,
                    null
                ) ?: return@runCatching null

            /**
             * Load the AES wrapping key from Android Keystore.
             *
             * Important:
             * We do NOT create a new key here.
             *
             * A newly generated AES key could never decrypt
             * ciphertext created with the old key.
             */
            val wrappingKey =
                getExistingWrappingKey()
                    ?: return@runCatching null

            /**
             * Decode Base64 strings back to binary data,
             * then decrypt using AES-GCM.
             */
            decrypt(
                cipherText = cipherTextBase64.fromBase64(),
                iv = ivBase64.fromBase64(),
                wrappingKey = wrappingKey
            )
                /**
                 * Convert back to UByteArray because our
                 * crypto layer uses unsigned byte arrays.
                 */
                .toUByteArray()
        }
    }

    /**
     * Loads and decrypts the Ed25519 private signing key.
     *
     * The flow is identical to loading the X25519 key,
     * but uses separate stored ciphertext and IV values.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun loadSigningPrivateKey(): Result<UByteArray?> {
        return runCatching {
            // Load encrypted Ed25519 private-key ciphertext.
            val cipherTextBase64 =
                preferences.getString(
                    SIGNING_PRIVATE_KEY_CIPHERTEXT,
                    null
                ) ?: return@runCatching null

            // Load the IV used during Ed25519 key encryption.
            val ivBase64 =
                preferences.getString(
                    SIGNING_PRIVATE_KEY_IV,
                    null
                ) ?: return@runCatching null

            /**
             * Retrieve the existing AES wrapping key.
             *
             * Again, never generate a replacement during loading.
             */
            val wrappingKey =
                getExistingWrappingKey()
                    ?: return@runCatching null

            // Decrypt and return the original Ed25519 private key.
            decrypt(
                cipherText = cipherTextBase64.fromBase64(),
                iv = ivBase64.fromBase64(),
                wrappingKey = wrappingKey
            ).toUByteArray()
        }
    }

    /**
     * Returns the existing AES wrapping key or creates it
     * if this is the first time the app needs one.
     *
     * This should normally happen once during first identity creation.
     */
    private fun getOrCreateWrappingKey(): SecretKey {
        /**
         * First check whether the key already exists.
         *
         * Reusing the existing key is required because previously
         * encrypted private keys depend on it.
         */
        getExistingWrappingKey()?.let { return it }

        /**
         * Create an AES KeyGenerator backed by Android Keystore.
         *
         * The generated key is managed by Android Keystore rather
         * than being returned as ordinary exportable key bytes.
         */
        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        /**
         * Define exactly what the key may be used for.
         */
        val keySpec =
            KeyGenParameterSpec
                .Builder(
                    // Name used to retrieve the key later.
                    WRAPPING_KEY_ALIAS,
                    /**
                     * Allow this key to:
                     *
                     * - encrypt
                     * - decrypt
                     */
                    KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
                )
                /**
                 * Use GCM mode.
                 *
                 * GCM provides authenticated encryption.
                 */
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                /**
                 * GCM does not use traditional block padding.
                 */
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                /**
                 * Request a 256-bit AES key.
                 */
                .setKeySize(256)
                .build()

        /**
         * Configure the generator with our security requirements.
         */
        keyGenerator.init(keySpec)

        /**
         * Generate the AES key inside Android Keystore.
         */
        return keyGenerator.generateKey()
    }

    /**
     * Retrieves the AES wrapping key if it already exists.
     *
     * Returns null if no key exists for the configured alias.
     */
    private fun getExistingWrappingKey(): SecretKey? {
        val key =
            keyStore.getKey(
                WRAPPING_KEY_ALIAS,
                null
            )

        return key as? SecretKey
    }

    /**
     * Encrypts arbitrary binary data using AES-GCM.
     *
     * In our case the binary data is a libsodium private key.
     */
    private fun encrypt(
        plainData: ByteArray,
        wrappingKey: SecretKey
    ): EncryptedBlob {
        /**
         * Request AES encryption using:
         *
         * AES
         * +
         * GCM mode
         * +
         * no padding
         */
        val cipher =
            Cipher.getInstance(
                AES_GCM_TRANSFORMATION
            )

        /**
         * Initialize encryption.
         *
         * Android's crypto provider generates a fresh IV.
         */
        cipher.init(
            Cipher.ENCRYPT_MODE,
            wrappingKey
        )

        /**
         * Encrypt the private-key bytes.
         *
         * AES-GCM also produces an authentication tag,
         * included in the resulting ciphertext representation.
         */
        val cipherText =
            cipher.doFinal(
                plainData
            )

        /**
         * Return:
         *
         * - encrypted bytes
         * - IV required for decryption
         */
        return EncryptedBlob(
            cipherText = cipherText,
            iv = cipher.iv
        )
    }

    /**
     * Decrypts AES-GCM protected data.
     */
    private fun decrypt(
        cipherText: ByteArray,
        iv: ByteArray,
        wrappingKey: SecretKey
    ): ByteArray {
        val cipher =
            Cipher.getInstance(
                AES_GCM_TRANSFORMATION
            )

        /**
         * Recreate the GCM parameters used for decryption.
         *
         * 128 = authentication tag length in bits.
         */
        val parameterSpec =
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                iv
            )

        /**
         * Initialize the cipher for decryption using:
         *
         * - same AES wrapping key
         * - original IV
         */
        cipher.init(
            Cipher.DECRYPT_MODE,
            wrappingKey,
            parameterSpec
        )

        /**
         * Decrypt and authenticate.
         *
         * If ciphertext or authentication data was modified,
         * this operation should fail instead of returning
         * silently corrupted plaintext.
         */
        return cipher.doFinal(
            cipherText
        )
    }

    /**
     * Converts binary data into Base64 text.
     *
     * SharedPreferences stores strings, not arbitrary byte arrays.
     *
     * NO_WRAP avoids unnecessary line breaks.
     */
    private fun ByteArray.toBase64(): String =
        Base64.encodeToString(
            this,
            Base64.NO_WRAP
        )

    /**
     * Converts Base64 text back into its original binary bytes.
     */
    private fun String.fromBase64(): ByteArray =
        Base64.decode(
            this,
            Base64.NO_WRAP
        )

    /**
     * Small internal container for one AES-GCM encryption result.
     *
     * Both values are required:
     *
     * cipherText
     *     = encrypted private key
     *
     * iv
     *     = initialization vector needed for decryption
     */
    private data class EncryptedBlob(
        val cipherText: ByteArray,
        val iv: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedBlob

            if (!cipherText.contentEquals(other.cipherText)) return false
            if (!iv.contentEquals(other.iv)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = cipherText.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }

    /**
     * Constants are kept together to avoid duplicated
     * security-sensitive strings throughout the class.
     */
    private companion object {
        /**
         * Name of Android's special KeyStore provider.
         */
        const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /**
         * Alias under which our AES wrapping key is stored.
         *
         * This is a reference/name, not the actual AES key.
         */
        const val WRAPPING_KEY_ALIAS = "securechat_identity_wrapping_key"

        /**
         * Authenticated encryption transformation.
         */
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"

        /**
         * AES-GCM authentication tag length.
         */
        const val GCM_TAG_LENGTH_BITS = 128

        /**
         * Name of our app-private SharedPreferences file.
         */
        const val PREFERENCES_NAME = "securechat_private_key_storage"

        /**
         * Storage key for encrypted X25519 private-key bytes.
         */
        const val ENCRYPTION_PRIVATE_KEY_CIPHERTEXT = "encryption_private_key_ciphertext"

        /**
         * Storage key for the X25519 encryption IV.
         */
        const val ENCRYPTION_PRIVATE_KEY_IV = "encryption_private_key_iv"

        /**
         * Storage key for encrypted Ed25519 private-key bytes.
         */
        const val SIGNING_PRIVATE_KEY_CIPHERTEXT = "signing_private_key_ciphertext"

        /**
         * Storage key for the Ed25519 encryption IV.
         */
        const val SIGNING_PRIVATE_KEY_IV = "signing_private_key_iv"
    }

    override suspend fun deleteIdentityPrivateKeys(): Result<Unit> =
        runCatching {
            /**
             * Remove every encrypted private-key component.
             *
             * We remove:
             * - X25519 ciphertext
             * - X25519 IV
             * - Ed25519 ciphertext
             * - Ed25519 IV
             *
             * No plaintext private key is stored here.
             */
            val deleted =
                preferences
                    .edit()
                    .remove(ENCRYPTION_PRIVATE_KEY_CIPHERTEXT)
                    .remove(ENCRYPTION_PRIVATE_KEY_IV)
                    .remove(SIGNING_PRIVATE_KEY_CIPHERTEXT)
                    .remove(SIGNING_PRIVATE_KEY_IV)
                    .commit()

            /**
             * commit() returns false when persistence fails.
             *
             * check() throws, and runCatching converts that
             * exception into Result.failure().
             */
            check(deleted) {
                "Failed to delete encrypted identity private keys"
            }

            if (keyStore.containsAlias(WRAPPING_KEY_ALIAS)) {
                keyStore.deleteEntry(WRAPPING_KEY_ALIAS)
            }
        }
}
