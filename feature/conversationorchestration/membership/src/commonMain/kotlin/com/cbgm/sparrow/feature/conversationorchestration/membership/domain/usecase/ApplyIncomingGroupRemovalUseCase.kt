package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupIncomingConversationRepository
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

/** Applies conversation changes only after the incoming workflow authorizes removal. */
class ApplyIncomingGroupRemovalUseCase(
    private val repository: GroupIncomingConversationRepository
) {
    suspend operator fun invoke(packet: GroupMemberRemovedPacket): Result<Unit> = repository.applyRemoval(packet)
}
