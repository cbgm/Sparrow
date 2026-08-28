package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.contacts.domain.usecase.HandleDirectChatAuthorizationRevokedPacketUseCase

class DirectChatAuthorizationRevokedPacketHandler(
    private val handleDirectChatAuthorizationRevokedPacket: HandleDirectChatAuthorizationRevokedPacketUseCase
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is DirectChatAuthorizationRevokedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        handleDirectChatAuthorizationRevokedPacket(
            context = context,
            packet =
                packet as? DirectChatAuthorizationRevokedPacket
                    ?: error("Incompatible direct chat authorization revocation packet")
        )
}
