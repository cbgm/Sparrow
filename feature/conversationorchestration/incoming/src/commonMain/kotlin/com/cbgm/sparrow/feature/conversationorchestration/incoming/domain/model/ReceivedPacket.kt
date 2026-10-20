package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model

import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.SparrowPacket

/** A decoded packet together with the sender and transport it arrived with. */
data class ReceivedPacket(
    val context: IncomingPacketContext,
    val packet: SparrowPacket
)
