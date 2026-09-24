package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.ApprovedIdentityReconnection
import com.cbgm.sparrow.feature.identity.domain.repository.ApprovedIdentityReconnectionRepository

class GetApprovedIdentityReconnectionUseCase(
    private val repository: ApprovedIdentityReconnectionRepository
) {
    suspend operator fun invoke(peerId: String): Result<ApprovedIdentityReconnection?> = repository.find(peerId)
}
