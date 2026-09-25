package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.LocalIdentitySharingRepository
import kotlinx.coroutines.flow.Flow

class ObserveLocalIdentitySharedUseCase(
    private val repository: LocalIdentitySharingRepository
) {
    operator fun invoke(peerId: String): Flow<Boolean> = repository.observeSharedWith(peerId)
}
