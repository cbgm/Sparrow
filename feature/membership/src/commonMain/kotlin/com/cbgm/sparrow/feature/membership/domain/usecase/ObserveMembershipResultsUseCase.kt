package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import kotlinx.coroutines.flow.Flow

class ObserveMembershipResultsUseCase(
    private val repository: MembershipRepository
) {
    operator fun invoke(): Flow<List<MembershipResult>> =
        repository.observeResults()
}
