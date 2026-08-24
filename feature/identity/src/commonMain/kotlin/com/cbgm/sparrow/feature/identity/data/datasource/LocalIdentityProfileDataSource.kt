package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

class LocalIdentityProfileDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observePhoneNumber(): Flow<String?> = dataStore.observeString(LOCAL_PHONE_NUMBER)

    suspend fun loadPhoneName(): Result<Pair<String, String>?> =
        runCatching {
            val phone = dataStore.getString(LOCAL_PHONE_NUMBER)
            val name = dataStore.getString(LOCAL_NAME)
            if (phone == null || name == null) null else phone to name
        }

    suspend fun loadPhoneNumber(): Result<String?> =
        runCatching { dataStore.getString(LOCAL_PHONE_NUMBER) }

    suspend fun savePhoneName(
        phoneNumber: String,
        name: String
    ): Result<Unit> =
        runCatching {
            dataStore.edit {
                putString(LOCAL_PHONE_NUMBER, phoneNumber)
                putString(LOCAL_NAME, name)
            }
        }

    suspend fun deletePhoneName(): Result<Unit> =
        runCatching {
            dataStore.edit {
                removeString(LOCAL_PHONE_NUMBER)
                removeString(LOCAL_NAME)
            }
        }

    private companion object {
        const val PREFIX = "identity.profile."
        const val LOCAL_PHONE_NUMBER = "${PREFIX}phone_number"
        const val LOCAL_NAME = "${PREFIX}name"
    }
}
