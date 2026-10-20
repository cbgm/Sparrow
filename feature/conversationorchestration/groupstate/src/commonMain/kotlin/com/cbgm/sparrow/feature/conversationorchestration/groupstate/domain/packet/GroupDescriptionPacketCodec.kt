package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState
import com.cbgm.sparrow.protocol.packet.GroupDescriptionUpdatedPacket

/** Signs and verifies the admin-signed packet that synchronizes the group description. */
interface GroupDescriptionPacketCodec {
    suspend fun create(state: GroupDescriptionState, context: GroupAdminContext): Result<GroupDescriptionUpdatedPacket>

    suspend fun verify(packet: GroupDescriptionUpdatedPacket): Result<Unit>
}
