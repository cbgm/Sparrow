package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class StartGroupMembershipUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        title: String,
        peerId: String
    ): Result<StartedMembershipHandshake> =
        repository.startHandshake(groupId, title, peerId)
}
