package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlin.io.encoding.Base64

/** The status belongs to the current signing public key; an old exported identity cannot mark new keys as backed up. */
class IdentityBackupStatusDataSource(
    private val dataStore: SparrowDataStore
) {
    suspend fun get(signingPublicKey: ByteArray): String? {
        if (dataStore.getString(KEY_IDENTITY) != Base64.encode(signingPublicKey)) return null
        return dataStore.getString(KEY_STATUS)
    }

    suspend fun set(signingPublicKey: ByteArray, status: String) {
        dataStore.edit {
            putString(KEY_IDENTITY, Base64.encode(signingPublicKey))
            putString(KEY_STATUS, status)
        }
    }

    private companion object {
        const val KEY_IDENTITY = "identity.backup.signing_public_key"
        const val KEY_STATUS = "identity.backup.status"
    }
}
