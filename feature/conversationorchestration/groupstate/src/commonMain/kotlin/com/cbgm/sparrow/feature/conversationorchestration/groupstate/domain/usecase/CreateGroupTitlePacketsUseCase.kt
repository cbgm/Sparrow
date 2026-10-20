package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupTitlePacketCodec
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket

/** Signs one group title packet per recipient. */
class CreateGroupTitlePacketsUseCase(
    private val packetCodec: GroupTitlePacketCodec
) {
    suspend operator fun invoke(
        state: GroupTitleState,
        context: GroupAdminContext,
        recipients: Set<String> = context.recipientContactIds
    ): Result<Map<String, GroupTitleUpdatedPacket>> =
        safeSuspendCall(logFailure = false) {
            recipients.associateWith { packetCodec.create(state, context).getOrThrow() }
        }
}
