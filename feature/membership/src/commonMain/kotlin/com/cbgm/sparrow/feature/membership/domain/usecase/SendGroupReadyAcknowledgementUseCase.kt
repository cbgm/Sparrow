package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class SendGroupReadyAcknowledgementUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        epoch: Int,
        welcomePacketId: String,
        recipientContactId: String
    ): Result<Unit> = repository.sendGroupReadyAcknowledgement(
        groupId = groupId,
        epoch = epoch,
        welcomePacketId = welcomePacketId,
        recipientContactId = recipientContactId
    )
}
