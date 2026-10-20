package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet.GroupPinPacketCodec
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket

/** Signs one pin packet per recipient. */
class CreateGroupPinPacketsUseCase(
    private val pinPacketCodec: GroupPinPacketCodec
) {
    suspend operator fun invoke(
        pin: GroupPinState,
        context: GroupAdminContext,
        recipients: Set<String> = context.recipientContactIds
    ): Result<Map<String, GroupPinUpdatedPacket>> =
        safeSuspendCall(logFailure = false) {
            recipients.associateWith { pinPacketCodec.create(pin, context).getOrThrow() }
        }
}
