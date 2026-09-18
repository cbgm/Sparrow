package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow

class ObserveMembershipResultsUseCase(
    private val repository: GroupMembershipRepository
) {
    operator fun invoke(): Flow<List<MembershipResult>> =
        repository.observeMembershipResults()
}
