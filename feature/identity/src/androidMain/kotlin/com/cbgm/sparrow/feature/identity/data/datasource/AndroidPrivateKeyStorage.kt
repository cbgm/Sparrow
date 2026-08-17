package com.cbgm.sparrow.feature.identity.data.datasource

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.cbgm.sparrow.core.datastore.SparrowDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidPrivateKeyStorage(
    private val dataStore: SparrowDataStore
) : PrivateKeyStorage {
    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun saveIdentityPrivateKeys(
        encryptionPrivateKey: UByteArray,
        signingPrivateKey: UByteArray
    ): Result<Unit> =
        runCatching {
            val wrappingKey = getOrCreateWrappingKey()
            val encryptedEncryptionKey = encrypt(encryptionPrivateKey.toByteArray(), wrappingKey)
            val encryptedSigningKey = encrypt(signingPrivateKey.toByteArray(), wrappingKey)

            dataStore.edit {
                putString(ENCRYPTION_PRIVATE_KEY_CIPHERTEXT, encryptedEncryptionKey.cipherText.toBase64())
                putString(ENCRYPTION_PRIVATE_KEY_IV, encryptedEncryptionKey.iv.toBase64())
                putString(SIGNING_PRIVATE_KEY_CIPHERTEXT, encryptedSigningKey.cipherText.toBase64())
                putString(SIGNING_PRIVATE_KEY_IV, encryptedSigningKey.iv.toBase64())
            }
        }

    override suspend fun hasIdentityPrivateKeys(): Result<Boolean> =
        runCatching {
            val stored =
                dataStore.containsString(ENCRYPTION_PRIVATE_KEY_CIPHERTEXT) &&
                    dataStore.containsString(ENCRYPTION_PRIVATE_KEY_IV) &&
                    dataStore.containsString(SIGNING_PRIVATE_KEY_CIPHERTEXT) &&
                    dataStore.containsString(SIGNING_PRIVATE_KEY_IV)

            stored &&
                getExistingWrappingKey() != null &&
                loadEncryptionPrivateKey().getOrNull() != null &&
                loadSigningPrivateKey().getOrNull() != null
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun loadEncryptionPrivateKey(): Result<UByteArray?> =
        loadPrivateKey(
            cipherTextKey = ENCRYPTION_PRIVATE_KEY_CIPHERTEXT,
            ivKey = ENCRYPTION_PRIVATE_KEY_IV
        )

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun loadSigningPrivateKey(): Result<UByteArray?> =
        loadPrivateKey(
            cipherTextKey = SIGNING_PRIVATE_KEY_CIPHERTEXT,
            ivKey = SIGNING_PRIVATE_KEY_IV
        )

    override suspend fun deleteIdentityPrivateKeys(): Result<Unit> =
        runCatching {
            dataStore.edit {
                removeString(ENCRYPTION_PRIVATE_KEY_CIPHERTEXT)
                removeString(ENCRYPTION_PRIVATE_KEY_IV)
                removeString(SIGNING_PRIVATE_KEY_CIPHERTEXT)
                removeString(SIGNING_PRIVATE_KEY_IV)
            }
            if (keyStore.containsAlias(WRAPPING_KEY_ALIAS)) {
                keyStore.deleteEntry(WRAPPING_KEY_ALIAS)
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun loadPrivateKey(
        cipherTextKey: String,
        ivKey: String
    ): Result<UByteArray?> =
        runCatching {
            val cipherText = dataStore.getString(cipherTextKey)?.fromBase64() ?: return@runCatching null
            val iv = dataStore.getString(ivKey)?.fromBase64() ?: return@runCatching null
            val wrappingKey = getExistingWrappingKey() ?: return@runCatching null
            decrypt(cipherText, iv, wrappingKey).toUByteArray()
        }

    private fun getOrCreateWrappingKey(): SecretKey =
        getExistingWrappingKey()
            ?: KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                .apply {
                    init(
                        KeyGenParameterSpec
                            .Builder(
                                WRAPPING_KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                            ).setKeySize(WRAPPING_KEY_SIZE_BITS)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build()
                    )
                }.generateKey()

    private fun getExistingWrappingKey(): SecretKey? =
        (keyStore.getEntry(WRAPPING_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun encrypt(
        plainData: ByteArray,
        wrappingKey: SecretKey
    ): EncryptedBlob {
        val cipher =
            Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, wrappingKey)
            }
        return EncryptedBlob(
            cipherText = cipher.doFinal(plainData),
            iv = cipher.iv
        )
    }

    private fun decrypt(
        cipherText: ByteArray,
        iv: ByteArray,
        wrappingKey: SecretKey
    ): ByteArray =
        Cipher
            .getInstance(AES_GCM_TRANSFORMATION)
            .apply {
                init(
                    Cipher.DECRYPT_MODE,
                    wrappingKey,
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                )
            }.doFinal(cipherText)

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private data class EncryptedBlob(
        val cipherText: ByteArray,
        val iv: ByteArray
    )

    private companion object {
        const val PREFIX = "identity.private."
        const val ENCRYPTION_PRIVATE_KEY_CIPHERTEXT = "${PREFIX}encryption_ciphertext"
        const val ENCRYPTION_PRIVATE_KEY_IV = "${PREFIX}encryption_iv"
        const val SIGNING_PRIVATE_KEY_CIPHERTEXT = "${PREFIX}signing_ciphertext"
        const val SIGNING_PRIVATE_KEY_IV = "${PREFIX}signing_iv"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val WRAPPING_KEY_ALIAS = "sparrow_identity_wrapping_key"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val WRAPPING_KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
