package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator

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
