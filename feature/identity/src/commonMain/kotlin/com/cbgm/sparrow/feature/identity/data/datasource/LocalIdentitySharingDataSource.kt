package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persist share progress per peer and per *local identity* so a key reset cannot reuse it. */
internal class LocalIdentitySharingDataSource(
    private val store: SparrowDataStore,
    private val localIdentity: LocalPublicIdentityProvider
) {
    fun observeSharedWith(peerId: String): Flow<Boolean> {
        require(peerId.isNotBlank())
        return store.observeString(key(peerId)).map { storedIdentity ->
            storedIdentity != null && storedIdentity == currentIdentityFingerprint()
        }
    }

    suspend fun recordSharedWith(peerId: String) {
        require(peerId.isNotBlank())
        val fingerprint = currentIdentityFingerprint() ?: error("Local identity is not initialized")
        store.edit { putString(key(peerId), fingerprint) }
    }

    private suspend fun currentIdentityFingerprint(): String? =
        localIdentity.getLocalPublicIdentity().getOrNull()?.signingPublicKey
            ?.joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }

    private fun key(peerId: String) = "identity.shared_with.${peerId.encodeToByteArray().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }}"
}
