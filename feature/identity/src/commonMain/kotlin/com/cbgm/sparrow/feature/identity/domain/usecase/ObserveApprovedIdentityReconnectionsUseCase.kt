package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.repository.ApprovedIdentityReconnectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveApprovedIdentityReconnectionsUseCase(
    private val repository: ApprovedIdentityReconnectionRepository
) {
    operator fun invoke(): Flow<List<ApprovedIdentityReconnection>> = repository.observeAll()
}
