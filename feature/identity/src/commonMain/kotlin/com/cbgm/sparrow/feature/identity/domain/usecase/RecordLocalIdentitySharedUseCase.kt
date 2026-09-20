package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentitySharingRepository

class RecordLocalIdentitySharedUseCase(
    private val repository: LocalIdentitySharingRepository
) {
    suspend operator fun invoke(peerId: String): Result<Unit> = runCatching {
        repository.recordSharedWith(peerId)
    }
}
