package com.cbgm.sparrow.protocol.codec

import com.cbgm.sparrow.protocol.packet.SparrowPacket

interface PacketCodec {
    fun encode(packet: SparrowPacket): Result<ByteArray>

    fun decode(encodedPacket: ByteArray): Result<SparrowPacket>
}
