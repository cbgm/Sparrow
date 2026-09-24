package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationDeclineProtocol
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import kotlinx.coroutines.flow.first

/** Decline the signed identity-change offer with the SAME signed reply as a normal
 * direct invitation, then discard this exact candidate without changing trusted keys
 * or local history. Legacy candidates with no original challenge are local-only. */
class DeclinePendingRemoteIdentityChangeUseCase(
    private val observePending: ObservePendingRemoteIdentityChangesUseCase,
    private val dismiss: DismissPendingRemoteIdentityChangeUseCase,
    private val signingKeys: LocalSigningKeyPairProvider,
    private val protocol: ContactInvitationDeclineProtocol,
    private val outbox: ProtocolOutbox
) {
    suspend operator fun invoke(peerId: String, invitationId: String): Result<Unit> = safeSuspendCall {
        val candidate = observePending().first().firstOrNull {
            it.peerId == peerId && it.invitationId == invitationId
        } ?: return@safeSuspendCall
        val challenge = candidate.originalInviteChallenge
        if (challenge != null && candidate.expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
            val packet = protocol.createPacket(
                exchangeId = candidate.invitationId,
                inviteChallenge = challenge,
                declinedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                signingKeyPair = signingKeys.getSigningKeyPair().getOrThrow()
            ).getOrThrow()
            if (outbox.findByPacketId(packet.packetId).getOrThrow() == null) {
                outbox.enqueue(peerId, packet).getOrThrow()
            } else {
                outbox.resend(packet.packetId).getOrThrow()
            }
        }
        dismiss(peerId, invitationId).getOrThrow()
    }
}
