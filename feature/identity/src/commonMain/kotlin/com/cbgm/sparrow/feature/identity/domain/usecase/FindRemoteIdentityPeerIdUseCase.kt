package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository

class FindRemoteIdentityPeerIdUseCase(
    private val repository: RemoteIdentityReadRepository
) {
    suspend operator fun invoke(signingPublicKey: ByteArray): Result<String?> =
        repository.findPeerIdBySigningPublicKey(signingPublicKey)
}
