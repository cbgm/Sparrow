package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.util.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.util.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.flow.Flow

class LocalIdentityProfileDataSource(
    private val dataStore: SparrowDataStore,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : LocalPhoneNumberProvider {
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

    // The verification token lives in memory only; it is never stored.
    private var verificationToken: String? = null
    private var verificationTokenReceivedAt = 0L

    fun keepPhoneVerificationToken(token: String) {
        verificationToken = token
        verificationTokenReceivedAt = SystemClock.nowEpochMilliseconds()
    }

    /** The token is only valid for a limited time; an expired one is dropped instead of sent. */
    override suspend fun getPhoneVerificationToken(): Result<String?> =
        safeSuspendCall {
            val age = SystemClock.nowEpochMilliseconds() - verificationTokenReceivedAt
            if (age !in 0..TOKEN_LIFETIME_MILLISECONDS) verificationToken = null
            verificationToken
        }

    override suspend fun clearPhoneVerificationToken(): Result<Unit> =
        safeSuspendCall { verificationToken = null }

    override suspend fun getLocalPhoneNumber(): Result<String> =
        runCatching {
            val storedPhoneNumber =
                loadPhoneNumber()
                    ?.takeIf { it.isNotBlank() }
                    ?: error("Local phone number has not been configured")

            phoneNumberNormalizer.normalize(phoneNumber = storedPhoneNumber).getOrThrow()
        }

    private companion object {
        const val PREFIX = "identity.profile."
        const val LOCAL_PHONE_NUMBER = "${PREFIX}phone_number"
        const val LOCAL_NAME = "${PREFIX}name"

        // Firebase ID tokens expire after one hour; keep a margin for the clock and the request.
        const val TOKEN_LIFETIME_MILLISECONDS = 50L * 60L * 1_000L
    }
}
