package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupAvatarPacketCodec
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket

/** Signs one group avatar packet per recipient. */
class CreateGroupAvatarPacketsUseCase(
    private val packetCodec: GroupAvatarPacketCodec
) {
    suspend operator fun invoke(
        state: GroupAvatarState,
        context: GroupAdminContext,
        recipients: Set<String> = context.recipientContactIds
    ): Result<Map<String, GroupAvatarUpdatedPacket>> =
        runCatching {
            recipients.associateWith { packetCodec.create(state, context).getOrThrow() }
        }
}
