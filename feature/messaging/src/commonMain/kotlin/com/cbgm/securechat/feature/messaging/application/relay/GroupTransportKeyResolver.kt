package com.cbgm.securechat.feature.messaging.application.relay

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

interface GroupTransportKeyResolver {
    suspend fun resolveEncryptionPublicKey(
        packet: SecureChatPacket,
        contactId: String
    ): Result<ByteArray?>
}
