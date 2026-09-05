package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import kotlin.io.encoding.Base64

class SparrowDataStorePublicIdentityDataSource(
    private val dataStore: SparrowDataStore
) : PublicIdentityDataSource {
    override suspend fun save(identity: PublicIdentity) {
        dataStore.edit {
            putString(ENCRYPTION_PUBLIC_KEY, Base64.encode(identity.encryptionPublicKey))
            putString(SIGNING_PUBLIC_KEY, Base64.encode(identity.signingPublicKey))
        }
    }

    override suspend fun load(): PublicIdentity? {
        val encryption = dataStore.getString(ENCRYPTION_PUBLIC_KEY) ?: return null
        val signing = dataStore.getString(SIGNING_PUBLIC_KEY) ?: return null
        return PublicIdentity(
            encryptionPublicKey = Base64.decode(encryption),
            signingPublicKey = Base64.decode(signing)
        )
    }

    override suspend fun exists(): Boolean = dataStore.containsString(ENCRYPTION_PUBLIC_KEY) && dataStore.containsString(SIGNING_PUBLIC_KEY)

    override suspend fun delete() {
        dataStore.edit {
            removeString(ENCRYPTION_PUBLIC_KEY)
            removeString(SIGNING_PUBLIC_KEY)
        }
    }

    private companion object {
        const val PREFIX = "identity.public."
        const val ENCRYPTION_PUBLIC_KEY = "${PREFIX}encryption_key"
        const val SIGNING_PUBLIC_KEY = "${PREFIX}signing_key"
    }
}
