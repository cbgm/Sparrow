package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class VerifyGroupKeyConfirmationUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String, epoch: Int, confirmation: ByteArray): Result<Unit> =
        repository.verifyGroupKeyConfirmation(groupId, epoch, confirmation)
}
