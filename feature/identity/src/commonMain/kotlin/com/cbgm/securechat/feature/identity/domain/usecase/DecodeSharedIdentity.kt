package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec

class DecodeSharedIdentity(
    private val identityShareCodec: IdentityShareCodec
) {
    operator fun invoke(encodedIdentity: String): Result<SharedIdentityPayload> =
        identityShareCodec.decode(encodedIdentity)
}
