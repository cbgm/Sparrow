package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import kotlinx.coroutines.flow.Flow

class ObservePendingRemoteIdentityChangesUseCase(
    private val repository: PendingRemoteIdentityChangeRepository
) {
    operator fun invoke(): Flow<List<PendingRemoteIdentityChange>> = repository.observeAll()
}
