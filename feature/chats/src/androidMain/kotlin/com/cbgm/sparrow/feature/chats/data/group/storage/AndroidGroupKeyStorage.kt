package com.cbgm.sparrow.feature.chats.data.group.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Protects group epoch keys with an AES-256 key held by Android Keystore.
 * SharedPreferences contains only AES-GCM ciphertext and IV values.
 */
class AndroidGroupKeyStorage(
    context: Context
) {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    private val keyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

    suspend fun save(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): Result<Unit> =
        runCatching {
            requireGroupReference(groupId, epoch)
            require(groupKey.size == GROUP_KEY_SIZE) { "Group key must be $GROUP_KEY_SIZE bytes" }

            val storageId = storageId(groupId, epoch)
            val cipher =
                Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                    init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
                    updateAAD(storageId.encodeToByteArray())
                }
            val encrypted = cipher.doFinal(groupKey)
            val saved =
                preferences
                    .edit()
                    .putString(ciphertextPreference(storageId), encrypted.toBase64())
                    .putString(ivPreference(storageId), cipher.iv.toBase64())
                    .commit()

            check(saved) { "Encrypted group key could not be persisted" }
        }

    suspend fun load(
        groupId: String,
        epoch: Int
    ): Result<ByteArray?> =
        runCatching {
            requireGroupReference(groupId, epoch)

            val storageId = storageId(groupId, epoch)
            val encrypted =
                preferences.getString(ciphertextPreference(storageId), null)
                    ?: return@runCatching null
            val iv =
                preferences.getString(ivPreference(storageId), null)
                    ?: return@runCatching null
            val wrappingKey = getExistingWrappingKey() ?: return@runCatching null
            val cipher =
                Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                    init(
                        Cipher.DECRYPT_MODE,
                        wrappingKey,
                        GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv.fromBase64())
                    )
                    updateAAD(storageId.encodeToByteArray())
                }

            cipher
                .doFinal(encrypted.fromBase64())
                .also { groupKey ->
                    check(groupKey.size == GROUP_KEY_SIZE) {
                        "Stored group key has an invalid length"
                    }
                }
        }

    suspend fun deleteBefore(
        groupId: String,
        epoch: Int
    ): Result<Unit> =
        runCatching {
            requireGroupReference(groupId, epoch)

            val editor = preferences.edit()
            preferences.all.keys
                .filter { preferenceKey ->
                    preferenceKey.startsWith("$ENTRY_PREFIX$groupId:") &&
                        preferenceKey.epochOrNull()?.let { storedEpoch -> storedEpoch < epoch } == true
                }.forEach { preferenceKey ->
                    editor.remove(preferenceKey)
                }

            check(editor.commit()) { "Old group keys could not be removed" }
        }

    suspend fun deleteGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            val editor = preferences.edit()
            preferences.all.keys
                .filter { preferenceKey ->
                    preferenceKey.startsWith("$ENTRY_PREFIX$groupId:")
                }.forEach { preferenceKey ->
                    editor.remove(preferenceKey)
                }

            check(editor.commit()) { "Group keys could not be removed" }
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

    private fun getExistingWrappingKey(): SecretKey? = (keyStore.getEntry(WRAPPING_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun requireGroupReference(
        groupId: String,
        epoch: Int
    ) {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
    }

    private fun storageId(
        groupId: String,
        epoch: Int
    ): String = "$ENTRY_PREFIX$groupId:$epoch"

    private fun ciphertextPreference(storageId: String): String = "$storageId:ciphertext"

    private fun ivPreference(storageId: String): String = "$storageId:iv"

    private fun String.epochOrNull(): Int? =
        removePrefix(ENTRY_PREFIX)
            .substringAfter(':', missingDelimiterValue = "")
            .substringBefore(':')
            .toIntOrNull()

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val PREFERENCES_NAME = "sparrow_group_keys"
        const val WRAPPING_KEY_ALIAS = "sparrow_group_key_wrapping_key"
        const val ENTRY_PREFIX = "group:"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val WRAPPING_KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val GROUP_KEY_SIZE = 32
    }
}
