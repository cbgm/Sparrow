package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class GetMembershipHandshakeUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(sourceId: String): Result<MembershipHandshake?> =
        repository.getHandshake(sourceId)
}
