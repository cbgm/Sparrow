package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class ClearMembershipHandshakeUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(sourceId: String): Result<Unit> =
        repository.clearHandshake(sourceId)
}
