package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import kotlinx.coroutines.flow.Flow

class ObserveRemoteIdentitiesUseCase(
    private val repository: RemoteIdentityReadRepository
) {
    operator fun invoke(): Flow<List<RemotePeerIdentity>> = repository.observeAll()
}
