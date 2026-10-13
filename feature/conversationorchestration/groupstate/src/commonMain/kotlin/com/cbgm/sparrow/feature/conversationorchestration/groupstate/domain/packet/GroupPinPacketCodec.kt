package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.packet

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.protocol.packet.GroupPinUpdatedPacket

/** Signs, verifies and reads the admin-signed packet that synchronizes a group's pin slot. */
interface GroupPinPacketCodec {
    suspend fun create(pin: GroupPinState, context: GroupAdminContext): Result<GroupPinUpdatedPacket>

    suspend fun verify(packet: GroupPinUpdatedPacket): Result<Unit>

    fun toGroupPinState(packet: GroupPinUpdatedPacket, senderContactId: String?, isMine: Boolean): GroupPinState
}
