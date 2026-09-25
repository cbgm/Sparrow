package com.cbgm.sparrow.core.protocol.authorization

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.time.SystemClock

/**
 * Owns delivery of the existing signed revocation packet. It does not decide whether
 * an exchange or conversation may be revoked, and never modifies feature state.
 */
class DirectChatAuthorizationRevocationSender(
    private val protocol: DirectChatAuthorizationRevocationProtocol,
    private val signingKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox
) {
    suspend fun enqueueOrResend(
        peerId: String,
        exchangeId: String,
        inviteChallenge: ByteArray
    ): Result<Unit> =
        runCatching {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            require(exchangeId.isNotBlank()) { "Exchange ID must not be blank" }
            val signingKeyPair = signingKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet = protocol.createPacket(
                invitationId = exchangeId,
                inviteChallenge = inviteChallenge,
                revokedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                signingKeyPair = signingKeyPair
            ).getOrThrow()
            // Keep the same durable outbox/resend semantics as the original Identity path.
            // A previously sent/failed packet is requeued instead of generating a second row.
            if (protocolOutbox.findByPacketId(packet.packetId).getOrThrow() == null) {
                protocolOutbox.enqueue(peerId, packet).getOrThrow()
            } else {
                protocolOutbox.resend(packet.packetId).getOrThrow()
            }
        }
}
