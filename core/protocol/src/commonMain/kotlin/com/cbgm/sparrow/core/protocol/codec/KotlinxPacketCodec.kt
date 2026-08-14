package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import kotlinx.serialization.json.Json

class KotlinxPacketCodec(
    private val json: Json
) : PacketCodec {
    override fun encode(packet: SparrowPacket): Result<ByteArray> =
        runCatching {
            require(ProtocolVersion.isSupported(packet.version)) {
                "Unsupported protocol version: ${packet.version}"
            }

            json
                .encodeToString(
                    serializer = SparrowPacket.serializer(),
                    value = packet
                ).encodeToByteArray()
        }

    override fun decode(encodedPacket: ByteArray): Result<SparrowPacket> =
        runCatching {
            require(encodedPacket.isNotEmpty()) {
                "Encoded packet must not be empty"
            }

            val encodedText = encodedPacket.decodeToString(throwOnInvalidSequence = true)

            val packet =
                json.decodeFromString(
                    deserializer = SparrowPacket.serializer(),
                    string = encodedText
                )

            require(ProtocolVersion.isSupported(packet.version)) {
                "Unsupported protocol version: ${packet.version}"
            }

            packet
        }
}
