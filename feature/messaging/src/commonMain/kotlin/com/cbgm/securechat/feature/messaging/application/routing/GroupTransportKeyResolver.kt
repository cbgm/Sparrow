package com.cbgm.securechat.feature.messaging.application.routing

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

interface GroupTransportKeyResolver {
    suspend fun resolveEncryptionPublicKey(
        packet: SecureChatPacket,
        contactId: String
    ): Result<ByteArray?>
}
