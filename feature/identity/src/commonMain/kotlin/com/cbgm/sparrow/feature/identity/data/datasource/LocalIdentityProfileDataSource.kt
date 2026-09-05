package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

class LocalIdentityProfileDataSource(
    private val dataStore: SparrowDataStore
) {
    fun observePhoneNumber(): Flow<String?> = dataStore.observeString(LOCAL_PHONE_NUMBER)

    suspend fun loadPhoneName(): Pair<String, String>? {
        val phone = dataStore.getString(LOCAL_PHONE_NUMBER)
        val name = dataStore.getString(LOCAL_NAME)
        return if (phone == null || name == null) null else phone to name
    }

    suspend fun loadPhoneNumber(): String? = dataStore.getString(LOCAL_PHONE_NUMBER)

    suspend fun savePhoneName(
        phoneNumber: String,
        name: String
    ) {
        dataStore.edit {
            putString(LOCAL_PHONE_NUMBER, phoneNumber)
            putString(LOCAL_NAME, name)
        }
    }

    suspend fun deletePhoneName() {
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
