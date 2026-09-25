package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class CompleteIncomingGroupRemovalUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(packet: GroupMemberRemovedPacket): Result<Unit> =
        repository.completeIncomingGroupRemoval(packet)
}
