package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupDescriptionPacketCodec
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket

/** Signs one group description packet per recipient. */
class CreateGroupDescriptionPacketsUseCase(
    private val packetCodec: GroupDescriptionPacketCodec
) {
    suspend operator fun invoke(
        state: GroupDescriptionState,
        context: GroupAdminContext,
        recipients: Set<String> = context.recipientContactIds
    ): Result<Map<String, GroupDescriptionUpdatedPacket>> =
        runCatching {
            recipients.associateWith { packetCodec.create(state, context).getOrThrow() }
        }
}
