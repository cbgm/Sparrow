package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository

class RecoverIncompleteIdentityUseCase(
    private val identityRepository: IdentityRepository,
    private val localIdentityChangeHandler: LocalIdentityChangeHandler
) {
    suspend operator fun invoke(): Result<Unit> =
        runCatching {
            check(identityRepository.getStatus().getOrThrow() == IdentityStatus.INCOMPLETE) {
                "Local identity recovery is only valid for incomplete identity state"
            }
            localIdentityChangeHandler.onLocalIdentityChanged().getOrThrow()
            identityRepository.resetIdentity().getOrThrow()
        }
}
