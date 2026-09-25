package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class MarkMembershipRemovedUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit> =
        repository.markRemoved(sourceId, updatedAtEpochMilliseconds)
}
