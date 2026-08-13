package com.cbgm.securechat.feature.identity.data.repository

import android.content.SharedPreferences
import com.cbgm.securechat.feature.identity.domain.repository.LocalIdentityProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocalIdentityProfileRepositoryImpl(
    private val preferences: SharedPreferences
) : LocalIdentityProfileRepository {
    override fun observePhoneNumber(): Flow<String?> =
        callbackFlow {
            trySend(preferences.getString(LOCAL_PHONE_NUMBER, null))

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->

                    if (key == LOCAL_PHONE_NUMBER) {
                        trySend(sharedPreferences.getString(LOCAL_PHONE_NUMBER, null))
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }

    override suspend fun loadPhoneName(): Result<Pair<String, String>?> =
        runCatching {
            val phone = preferences.getString(LOCAL_PHONE_NUMBER, null)
            val name = preferences.getString(LOCAL_NAME, null)

            if (phone == null || name == null) {
                null
            } else {
                phone to name
            }
        }

    override suspend fun loadPhoneNumber(): Result<String?> =
        runCatching {
            preferences.getString(LOCAL_PHONE_NUMBER, null)
        }

    override suspend fun savePhoneName(
        phoneNumber: String,
        name: String
    ): Result<Unit> =
        runCatching {
            require(phoneNumber.isNotBlank()) {
                "Phone number must not be blank"
            }

            val saved =
                preferences
                    .edit()
                    .putString(LOCAL_PHONE_NUMBER, phoneNumber)
                    .putString(LOCAL_NAME, name)
                    .commit()

            check(saved) {
                "Local phone and name could not be saved"
            }
        }

    override suspend fun deletePhoneName(): Result<Unit> =
        runCatching {
            val deleted =
                preferences
                    .edit()
                    .remove(LOCAL_PHONE_NUMBER)
                    .remove(LOCAL_NAME)
                    .commit()

            check(deleted) {
                "Local phone and name could not be deleted"
            }
        }

    private companion object {
        const val LOCAL_PHONE_NUMBER = "local_phone_number"
        const val LOCAL_NAME = "local_name"
    }
}
