package com.cbgm.sparrow.feature.conversationorchestration.domain.workflow

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding

/** A revocation signed by an unrecognized key cannot revoke the stored identity's exchange.
 * Call only after verifying the packet's detached signature. A mismatched signer is a
 * non-mutating, terminal result (not a reason to retry the same incoming envelope).
 */
internal enum class IncomingAuthorizationRevocationDecision {
    APPLY,
    IGNORE_UNRECOGNIZED_SIGNER
}

internal fun decideIncomingAuthorizationRevocation(
    context: IncomingPacketContext,
    packet: DirectChatAuthorizationRevokedPacket,
    binding: IdentityExchangeBinding
): IncomingAuthorizationRevocationDecision {
    check(packet.packetId == "direct-chat-authorization-revoked-${packet.invitationId}") {
        "Packet ID does not match the invitation transition"
    }
    check(binding.exchangeId == packet.invitationId && binding.peerId == context.contactId) {
        "Authorization revocation contact does not match invitation"
    }
    check(binding.inviteChallenge.contentEquals(packet.inviteChallenge)) {
        "Authorization revocation challenge does not match invitation"
    }
    require(packet.revokedAtEpochMilliseconds <= context.receivedAtEpochMilliseconds + 5L * 60L * 1_000L) {
        "Authorization revocation was created too far in the future"
    }
    return if (binding.remoteSigningPublicKey.contentEquals(packet.revokerSigningPublicKey)) {
        IncomingAuthorizationRevocationDecision.APPLY
    } else {
        IncomingAuthorizationRevocationDecision.IGNORE_UNRECOGNIZED_SIGNER
    }
}
