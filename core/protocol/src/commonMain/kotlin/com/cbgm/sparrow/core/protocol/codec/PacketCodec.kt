package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

interface PacketCodec {
    fun encode(packet: SparrowPacket): Result<ByteArray>

    fun decode(encodedPacket: ByteArray): Result<SparrowPacket>
}
