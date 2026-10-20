package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket

/** Signs and verifies the admin-signed packet that synchronizes the group title. */
interface GroupTitlePacketCodec {
    suspend fun create(state: GroupTitleState, context: GroupAdminContext): Result<GroupTitleUpdatedPacket>

    suspend fun verify(packet: GroupTitleUpdatedPacket): Result<Unit>
}
