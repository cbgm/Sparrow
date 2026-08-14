package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.domain.repository.IdentityShareRepository
import com.cbgm.securechat.feature.identity.domain.repository.LocalIdentityProfileRepository

/**
 * Creates the portable representation of the local SecureChat identity.
 *
 * The approved local phone number and both public keys are always
 * included. Only the display name is optional.
 */
class CreateSharedIdentityUseCase(
    private val getPublicIdentity: GetPublicIdentityUseCase,
    private val localIdentityProfileRepository: LocalIdentityProfileRepository,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val identityShareRepository: IdentityShareRepository
) {
    suspend operator fun invoke(): Result<String> =
        runCatching {
            val publicIdentity =
                getPublicIdentity().getOrThrow() ?: error("No public identity exists")

            val storedPhoneName =
                localIdentityProfileRepository.loadPhoneName().getOrThrow().takeIf { it != null }
                    ?: error("Local phone and name have not been configured")

            val normalizedPhoneNumber =
                phoneNumberNormalizer.normalize(phoneNumber = storedPhoneName.first).getOrThrow()

            val normalizedDisplayName = storedPhoneName.second

            identityShareRepository
                .encode(
                    payload =
                        SharedIdentityPayload(
                            version = 1,
                            encryptionPublicKey = publicIdentity.encryptionPublicKey.copyOf(),
                            signingPublicKey = publicIdentity.signingPublicKey.copyOf(),
                            contactDetails =
                                SharedContactDetails(
                                    displayName = normalizedDisplayName,
                                    phoneNumber = normalizedPhoneNumber
                                )
                        )
                ).getOrThrow()
        }
}
