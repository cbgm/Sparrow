package com.cbgm.sparrow.feature.identity.data.datasource

import android.content.Context
import android.util.Base64
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity

/**
 * Android implementation for storing the user's public identity.
 *
 * Public keys are not secret, so they do not need to be encrypted
 * with Android Keystore.
 *
 * We store them in app-private SharedPreferences as Base64 strings.
 */
class AndroidPublicIdentityStorage(
    context: Context
) : PublicIdentityStorage {
    /**
     * App-private SharedPreferences file used only for
     * the user's own public identity.
     */
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Stores both public keys:
     *
     * - encryption public key
     * - signing public key
     *
     * ByteArray values are converted to Base64 strings because
     * SharedPreferences cannot directly store ByteArray values.
     */
    override suspend fun save(identity: PublicIdentity): Result<Unit> =
        runCatching {
            val saved =
                preferences
                    .edit()
                    .putString(
                        ENCRYPTION_PUBLIC_KEY,
                        identity.encryptionPublicKey.toBase64()
                    ).putString(
                        SIGNING_PUBLIC_KEY,
                        identity.signingPublicKey.toBase64()
                    ).commit()

            /**
             * commit() returns false if persistence failed.
             *
             * check() throws in that case.
             * runCatching then converts the exception into Result.failure().
             */
            check(saved) {
                "Failed to persist public identity"
            }
        }

    /**
     * Loads the complete public identity.
     *
     * Returns:
     *
     * Result.success(PublicIdentity)
     *     if both keys exist.
     *
     * Result.success(null)
     *     if the identity is incomplete or missing.
     *
     * Result.failure(...)
     *     if decoding or another operation fails.
     */
    override suspend fun load(): Result<PublicIdentity?> {
        return runCatching {
            val encryptionPublicKeyBase64 =
                preferences.getString(
                    ENCRYPTION_PUBLIC_KEY,
                    null
                ) ?: return@runCatching null

            val signingPublicKeyBase64 =
                preferences.getString(
                    SIGNING_PUBLIC_KEY,
                    null
                ) ?: return@runCatching null

            PublicIdentity(
                encryptionPublicKey = encryptionPublicKeyBase64.fromBase64(),
                signingPublicKey = signingPublicKeyBase64.fromBase64()
            )
        }
    }

    /**
     * A public identity is considered present only when
     * both public keys exist.
     */
    override suspend fun exists(): Result<Boolean> =
        runCatching {
            preferences.contains(ENCRYPTION_PUBLIC_KEY) &&
                preferences.contains(SIGNING_PUBLIC_KEY)
        }

    /**
     * Converts binary key bytes into Base64 text.
     */
    private fun ByteArray.toBase64(): String =
        Base64.encodeToString(
            this,
            Base64.NO_WRAP
        )

    /**
     * Converts Base64 text back into binary key bytes.
     */
    private fun String.fromBase64(): ByteArray =
        Base64.decode(
            this,
            Base64.NO_WRAP
        )

    private companion object {
        const val PREFERENCES_NAME = "sparrow_public_identity_storage"

        const val ENCRYPTION_PUBLIC_KEY = "encryption_public_key"

        const val SIGNING_PUBLIC_KEY = "signing_public_key"
    }

    override suspend fun delete(): Result<Unit> =
        runCatching {
            /**
             * Remove both public identity keys.
             *
             * A public identity is complete only when both exist.
             */
            val deleted =
                preferences
                    .edit()
                    .remove(ENCRYPTION_PUBLIC_KEY)
                    .remove(SIGNING_PUBLIC_KEY)
                    .commit()

            check(deleted) {
                "Failed to delete public identity"
            }
        }
}
