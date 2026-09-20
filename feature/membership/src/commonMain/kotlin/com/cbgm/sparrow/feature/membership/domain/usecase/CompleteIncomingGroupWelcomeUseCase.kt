package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class CompleteIncomingGroupWelcomeUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String, senderContactId: String, isFirstWelcome: Boolean, removedContactIds: Set<String>, persistedAt: Long): Result<Unit> =
        repository.completeIncomingWelcome(groupId, senderContactId, isFirstWelcome, removedContactIds, persistedAt)
}
