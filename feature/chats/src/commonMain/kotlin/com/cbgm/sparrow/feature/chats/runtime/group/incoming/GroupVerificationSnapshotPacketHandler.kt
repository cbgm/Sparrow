package com.cbgm.sparrow.feature.chats.runtime.group.incoming

import com.cbgm.sparrow.feature.chats.data.group.incoming.handler.GroupPacketHandler
import com.cbgm.sparrow.feature.chats.runtime.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

class GroupVerificationSnapshotPacketHandler(
    private val coordinator: GroupVerificationCoordinator
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean =
        packet is GroupVerificationSnapshotPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        coordinator.receiveSnapshot(
            context = context,
            packet =
                packet as? GroupVerificationSnapshotPacket
                    ?: error("Incompatible group verification snapshot")
        )
}
