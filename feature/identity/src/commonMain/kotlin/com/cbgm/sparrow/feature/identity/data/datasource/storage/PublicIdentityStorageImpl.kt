package com.cbgm.sparrow.feature.identity.data.datasource.storage

import com.cbgm.sparrow.core.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import kotlin.io.encoding.Base64

class PublicIdentityStorageImpl(
    private val dataStore: SparrowDataStore
) : PublicIdentityStorage {
    override suspend fun save(identity: PublicIdentity): Result<Unit> =
        runCatching {
            dataStore.edit {
                putString(ENCRYPTION_PUBLIC_KEY, Base64.encode(identity.encryptionPublicKey))
                putString(SIGNING_PUBLIC_KEY, Base64.encode(identity.signingPublicKey))
            }
        }

    override suspend fun load(): Result<PublicIdentity?> =
        runCatching {
            val encryption = dataStore.getString(ENCRYPTION_PUBLIC_KEY) ?: return@runCatching null
            val signing = dataStore.getString(SIGNING_PUBLIC_KEY) ?: return@runCatching null
            PublicIdentity(
                encryptionPublicKey = Base64.decode(encryption),
                signingPublicKey = Base64.decode(signing)
            )
        }

    override suspend fun exists(): Result<Boolean> =
        runCatching {
            dataStore.containsString(ENCRYPTION_PUBLIC_KEY) && dataStore.containsString(SIGNING_PUBLIC_KEY)
        }

    override suspend fun delete(): Result<Unit> =
        runCatching {
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
