package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket

/** Signs and verifies the admin-signed packet that synchronizes the group avatar. */
interface GroupAvatarPacketCodec {
    suspend fun create(state: GroupAvatarState, context: GroupAdminContext): Result<GroupAvatarUpdatedPacket>

    suspend fun verify(packet: GroupAvatarUpdatedPacket): Result<Unit>
}
