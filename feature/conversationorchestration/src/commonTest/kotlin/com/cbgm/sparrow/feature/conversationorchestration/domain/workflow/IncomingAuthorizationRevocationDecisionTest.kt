package com.cbgm.sparrow.feature.conversationorchestration.domain.workflow

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IncomingAuthorizationRevocationDecisionTest {
    private val originalKey = ByteArray(32) { 1 }
    private val newKey = ByteArray(32) { 2 }
    private val challenge = ByteArray(32) { 3 }
    private val context = IncomingPacketContext(
        contactId = "peer-1",
        conversationId = "conversation-1",
        encodedTransportPayload = "encoded",
        transportMode = "encrypted",
        receivedAtEpochMilliseconds = 1_000_000L
    )
    private val binding = IdentityExchangeBinding(
        exchangeId = "invite-1",
        peerId = "peer-1",
        inviteChallenge = challenge,
        remoteSigningPublicKey = originalKey
    )

    private fun packet(
        key: ByteArray = originalKey,
        challengeBytes: ByteArray = challenge,
        packetId: String = "direct-chat-authorization-revoked-invite-1",
        timestamp: Long = 999_999L
    ) = DirectChatAuthorizationRevokedPacket(
        packetId = packetId,
        invitationId = "invite-1",
        revokedAtEpochMilliseconds = timestamp,
        inviteChallenge = challengeBytes,
        revokerSigningPublicKey = key,
        signature = ByteArray(64) { 4 }
    )

    @Test
    fun matchingPinnedSignerCanRevokeTheBoundInvitation() {
        assertEquals(
            IncomingAuthorizationRevocationDecision.APPLY,
            decideIncomingAuthorizationRevocation(context, packet(), binding)
        )
    }

    @Test
    fun replacedSignerCannotRevokePreviousIdentityExchange() {
        assertEquals(
            IncomingAuthorizationRevocationDecision.IGNORE_UNRECOGNIZED_SIGNER,
            decideIncomingAuthorizationRevocation(context, packet(key = newKey), binding)
        )
    }

    @Test
    fun wrongChallengeIsNotTreatedAsBenignStaleSigner() {
        assertFailsWith<IllegalStateException> {
            decideIncomingAuthorizationRevocation(
                context,
                packet(key = newKey, challengeBytes = ByteArray(32) { 9 }),
                binding
            )
        }
    }

    @Test
    fun wrongPeerIsNotTreatedAsBenignStaleSigner() {
        assertFailsWith<IllegalStateException> {
            decideIncomingAuthorizationRevocation(
                context.copy(contactId = "other-peer"),
                packet(key = newKey),
                binding
            )
        }
    }

    @Test
    fun wrongPacketIdAndFutureTimestampRemainInvalid() {
        assertFailsWith<IllegalStateException> {
            decideIncomingAuthorizationRevocation(context, packet(packetId = "unexpected"), binding)
        }
        assertFailsWith<IllegalArgumentException> {
            decideIncomingAuthorizationRevocation(
                context,
                packet(key = newKey, timestamp = 1_000_000L + 5L * 60L * 1_000L + 1L),
                binding
            )
        }
    }
}
