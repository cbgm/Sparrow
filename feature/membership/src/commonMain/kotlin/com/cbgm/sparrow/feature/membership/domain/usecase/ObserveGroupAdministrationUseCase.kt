package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupAdministrationUseCase(
    private val repository: GroupMembershipRepository
) {
    operator fun invoke(
        groupId: String
    ): Flow<GroupAdministrationState> =
        repository.observeAdministration(groupId)
}
