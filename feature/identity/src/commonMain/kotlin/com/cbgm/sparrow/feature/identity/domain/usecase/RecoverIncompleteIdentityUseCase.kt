package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.protocol.identity.LocalIdentityChangeHandler

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
