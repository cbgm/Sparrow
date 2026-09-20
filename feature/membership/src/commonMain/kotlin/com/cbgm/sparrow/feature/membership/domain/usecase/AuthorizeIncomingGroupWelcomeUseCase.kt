package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class AuthorizeIncomingGroupWelcomeUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String, senderContactId: String, packetId: String, epoch: Int): Result<GroupIncomingWelcomeAuthorization?> =
        repository.authorizeIncomingWelcome(groupId, senderContactId, packetId, epoch)
}
