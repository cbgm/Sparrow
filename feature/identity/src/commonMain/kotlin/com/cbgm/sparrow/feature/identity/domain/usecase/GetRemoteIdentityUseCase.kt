package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository

class GetRemoteIdentityUseCase(
    private val repository: RemoteIdentityReadRepository
) {
    suspend operator fun invoke(peerId: String): Result<RemotePeerIdentity?> = repository.get(peerId)
}
