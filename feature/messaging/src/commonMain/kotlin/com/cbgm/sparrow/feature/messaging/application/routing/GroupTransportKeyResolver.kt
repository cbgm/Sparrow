package com.cbgm.sparrow.feature.messaging.application.routing

import com.cbgm.sparrow.core.protocol.packet.SparrowPacket

interface GroupTransportKeyResolver {
    suspend fun resolveEncryptionPublicKey(
        packet: SparrowPacket,
        contactId: String
    ): Result<ByteArray?>
}
