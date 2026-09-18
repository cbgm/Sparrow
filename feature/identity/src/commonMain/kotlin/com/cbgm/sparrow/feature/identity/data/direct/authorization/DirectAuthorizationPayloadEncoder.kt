package com.cbgm.sparrow.feature.identity.data.direct.authorization

import com.cbgm.sparrow.core.crypto.util.ByteArrays

internal class DirectAuthorizationPayloadEncoder {
    fun encodeRevoked(
        packetId: String,
        version: Int,
        invitationId: String,
        revokedAtEpochMilliseconds: Long,
        inviteChallenge: ByteArray,
        revokerSigningPublicKey: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.withLengthPrefix("Sparrow.DirectChatAuthorizationRevoked".encodeToByteArray()),
            ByteArrays.withLengthPrefix(packetId.encodeToByteArray()),
            ByteArrays.withLengthPrefix(ByteArrays.encodeInt(version)),
            ByteArrays.withLengthPrefix(invitationId.encodeToByteArray()),
            ByteArrays.withLengthPrefix(ByteArrays.encodeLong(revokedAtEpochMilliseconds)),
            ByteArrays.withLengthPrefix(inviteChallenge),
            ByteArrays.withLengthPrefix(revokerSigningPublicKey)
        )
}
