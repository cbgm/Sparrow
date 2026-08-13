package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.domain.repository.IdentityShareRepository

class DecodeSharedIdentityUseCase(
    private val identityShareRepository: IdentityShareRepository
) {
    operator fun invoke(encodedIdentity: String): Result<SharedIdentityPayload> =
        identityShareRepository.decode(encodedIdentity)
}
