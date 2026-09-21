package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository

class StagePendingRemoteIdentityChangeUseCase(
    private val repository: PendingRemoteIdentityChangeRepository
) {
    suspend operator fun invoke(candidate: PendingRemoteIdentityChange): Result<Unit> = repository.stage(candidate)
}
