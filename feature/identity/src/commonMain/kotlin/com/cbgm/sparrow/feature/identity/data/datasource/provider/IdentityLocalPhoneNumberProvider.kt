package com.cbgm.sparrow.feature.identity.data.datasource.provider

import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentityProfileRepository

class IdentityLocalPhoneNumberProvider(
    private val localIdentityProfileRepository: LocalIdentityProfileRepository,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) : LocalPhoneNumberProvider {
    override suspend fun getLocalPhoneNumber(): Result<String> =
        runCatching {
            val storedPhoneNumber =
                localIdentityProfileRepository
                    .loadPhoneNumber()
                    .getOrThrow()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: error(
                        "Local phone number has not been configured"
                    )

            phoneNumberNormalizer.normalize(phoneNumber = storedPhoneNumber).getOrThrow()
        }
}
