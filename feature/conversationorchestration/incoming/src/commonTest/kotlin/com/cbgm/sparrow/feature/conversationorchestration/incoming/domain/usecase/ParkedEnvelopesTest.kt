package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DeferredPacketWait
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.DirectPeerAccess
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.IncomingEnvelopeHandling
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.model.ParkedEnvelope
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy.GroupPacketEpochPolicy
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.repository.ParkedEnvelopeRepository
import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation.FakeGroupMembershipRepository
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.PendingRemoteIdentityChange
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetIdentityPeerStateUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.FindGroupCurrentEpochUseCase
import com.cbgm.sparrow.feature.membership.domain.usecase.WasGroupMembershipDeletedUseCase
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.IdentityPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Packets that arrive too early (a group message before its welcome, a direct message before the invitation
 * completed) are parked as envelopes and processed again, in arrival order, once what they wait for happened.
 */
class ParkedEnvelopesTest {
    private val memberships = FakeGroupMembershipRepository()
    private val identities = FakeIdentities()
    private val blocklist = FakeBlocklist()
    private val parked = FakeParkedEnvelopes()
    private val findEpoch = FindGroupCurrentEpochUseCase(memberships)
    private val checkGroupReady = CheckGroupPacketReadyUseCase(findEpoch)
    private val resolveAccess =
        ResolveDirectPeerAccessUseCase(GetIdentityPeerStateUseCase(identities, NoPendingChanges()), blocklist)
    private val replay =
        ReplayParkedEnvelopesUseCase(
            parked,
            CheckDeferredPacketReadyUseCase(findEpoch, WasGroupMembershipDeletedUseCase(memberships), resolveAccess)
        )
    private val processed = mutableListOf<String>()

    @Test
    fun groupMessageBeforeTheWelcomeIsReplayedOnceItsEpochIsInstalled() = runBlocking {
        val wait = checkGroupReady(GROUP_ID, groupMessage(epoch = 2)).getOrThrow()
        assertEquals(DeferredPacketWait.GroupEpoch(GROUP_ID, 2), wait)
        park("envelope-1", wait!!)

        replayAll()
        memberships.currentEpochs[GROUP_ID] = 1
        replayAll()
        assertTrue(processed.isEmpty())

        memberships.currentEpochs[GROUP_ID] = 2
        replayAll()
        assertEquals(listOf("envelope-1"), processed)
        assertTrue(parked.envelopes.isEmpty())
        assertNull(checkGroupReady(GROUP_ID, groupMessage(epoch = 2)).getOrThrow())
    }

    @Test
    fun packetOfAnOlderEpochAndPacketsThatInstallOrEndEpochsDoNotWait() = runBlocking {
        memberships.currentEpochs[GROUP_ID] = 3
        assertNull(checkGroupReady(GROUP_ID, groupMessage(epoch = 2)).getOrThrow())

        memberships.currentEpochs.clear()
        assertNull(GroupPacketEpochPolicy.requiredEpoch(removal()))
        assertNull(checkGroupReady(GROUP_ID, removal()).getOrThrow())
    }

    @Test
    fun earlyDirectMessageWaitsUntilTheInvitationIsEstablished() = runBlocking {
        identities.states[CONTACT_ID] = IdentityPeerState(false, false, hasPendingExchange = true)
        assertEquals(DirectPeerAccess.PENDING, resolveAccess(CONTACT_ID).getOrThrow())
        park("envelope-1", DeferredPacketWait.ContactExchange(CONTACT_ID))

        replayAll()
        assertTrue(processed.isEmpty())

        identities.states[CONTACT_ID] = IdentityPeerState(true, false)
        replayAll()
        assertEquals(listOf("envelope-1"), processed)
        assertTrue(parked.envelopes.isEmpty())
    }

    @Test
    fun messagesOfUnknownBlockedOrRevokedSendersAreStillDropped() = runBlocking {
        identities.states[CONTACT_ID] = IdentityPeerState(false, false)
        assertEquals(DirectPeerAccess.DROPPED, resolveAccess(CONTACT_ID).getOrThrow())

        identities.states[CONTACT_ID] = IdentityPeerState(false, false, hasPendingExchange = true)
        blocklist.blocked += CONTACT_ID
        assertEquals(DirectPeerAccess.DROPPED, resolveAccess(CONTACT_ID).getOrThrow())
    }

    @Test
    fun onlyEnvelopesWhoseWaitIsMetAreReplayedInArrivalOrder() = runBlocking {
        identities.states[CONTACT_ID] = IdentityPeerState(false, false, hasPendingExchange = true)
        park("envelope-1", DeferredPacketWait.GroupEpoch(GROUP_ID, 2))
        park("envelope-2", DeferredPacketWait.ContactExchange(CONTACT_ID))
        park("envelope-3", DeferredPacketWait.GroupEpoch(GROUP_ID, 1))

        memberships.currentEpochs[GROUP_ID] = 2
        replayAll()
        assertEquals(listOf("envelope-1", "envelope-3"), processed)
        assertEquals(listOf("envelope-2"), parked.envelopes.map(ParkedEnvelope::envelopeId))
    }

    @Test
    fun envelopesOfADeletedGroupAreReplayedSoTheyAreDropped() = runBlocking {
        park("envelope-1", DeferredPacketWait.GroupEpoch(GROUP_ID, 2))

        memberships.deletedGroups += GROUP_ID
        replayAll(IncomingEnvelopeHandling.HANDLED)
        assertEquals(listOf("envelope-1"), processed)
        assertTrue(parked.envelopes.isEmpty())
    }

    @Test
    fun replayThatFailsOrWaitsAgainKeepsTheEnvelopeAndARejectionRemovesIt() = runBlocking {
        memberships.currentEpochs[GROUP_ID] = 2
        park("envelope-1", DeferredPacketWait.GroupEpoch(GROUP_ID, 2))

        replay { error("Database is busy") }.getOrThrow()
        assertEquals(1, parked.envelopes.size)

        replay { envelope ->
            parked.park(envelope.envelopeId, ROUTING_ID, PAYLOAD, DeferredPacketWait.GroupEpoch(GROUP_ID, 3))
            IncomingEnvelopeHandling.DEFERRED
        }.getOrThrow()
        assertEquals(DeferredPacketWait.GroupEpoch(GROUP_ID, 3), parked.envelopes.single().wait)

        memberships.currentEpochs[GROUP_ID] = 3
        replayAll(IncomingEnvelopeHandling.REJECTED)
        assertTrue(parked.envelopes.isEmpty())
    }

    @Test
    fun waitSurvivesTheRoundTripThroughItsKey() {
        val waits =
            listOf(DeferredPacketWait.GroupEpoch("group:with:colons", 12), DeferredPacketWait.ContactExchange("c-1"))
        assertEquals(waits, waits.map { DeferredPacketWait.fromKey(it.key) })
        assertNull(DeferredPacketWait.fromKey("unknown:key"))
    }

    private suspend fun park(envelopeId: String, wait: DeferredPacketWait) {
        parked.park(envelopeId, ROUTING_ID, PAYLOAD, wait).getOrThrow()
    }

    private suspend fun replayAll(handling: IncomingEnvelopeHandling = IncomingEnvelopeHandling.HANDLED) {
        replay { envelope ->
            processed += envelope.envelopeId
            handling
        }.getOrThrow()
    }

    private fun groupMessage(epoch: Int) =
        GroupChatMessagePacket(
            packetId = "packet",
            groupId = GROUP_ID,
            epoch = epoch,
            messageId = "message",
            sentAtEpochMilliseconds = 1,
            nonce = byteArrayOf(1),
            ciphertext = byteArrayOf(2),
            senderSignature = byteArrayOf(3)
        )

    private fun removal() =
        GroupMemberRemovedPacket(
            packetId = "packet",
            invitationId = "invitation",
            groupId = GROUP_ID,
            epoch = 4,
            challenge = byteArrayOf(1),
            removedMemberSigningPublicKey = byteArrayOf(2),
            removedAtEpochMilliseconds = 1,
            ownerSignature = byteArrayOf(3)
        )

    /** Ordered like the Room query; parking an envelope again keeps its place and only changes its wait. */
    private class FakeParkedEnvelopes : ParkedEnvelopeRepository {
        val envelopes = mutableListOf<ParkedEnvelope>()

        override suspend fun park(
            envelopeId: String,
            senderRoutingId: String,
            encodedTransportPayload: String,
            wait: DeferredPacketWait
        ): Result<Unit> {
            val index = envelopes.indexOfFirst { it.envelopeId == envelopeId }
            if (index >= 0) {
                envelopes[index] = envelopes[index].copy(wait = wait)
            } else {
                envelopes += ParkedEnvelope(envelopeId, senderRoutingId, encodedTransportPayload, wait, 0L)
            }
            return Result.success(Unit)
        }

        override suspend fun findWaits(): Result<List<DeferredPacketWait>> =
            Result.success(envelopes.map(ParkedEnvelope::wait).distinct())

        override suspend fun findWaitingFor(waits: List<DeferredPacketWait>): Result<List<ParkedEnvelope>> =
            Result.success(envelopes.filter { it.wait in waits })

        override suspend fun remove(envelopeId: String): Result<Unit> {
            envelopes.removeAll { it.envelopeId == envelopeId }
            return Result.success(Unit)
        }
    }

    private class FakeIdentities : IdentityExchangeRepository {
        val states = mutableMapOf<String, IdentityPeerState>()

        override suspend fun getPeerState(peerId: String): Result<IdentityPeerState> =
            Result.success(states[peerId] ?: IdentityPeerState(false, false))

        override suspend fun stageRemoteIdentity(
            peerId: String,
            encryptionPublicKey: ByteArray,
            signingPublicKey: ByteArray
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun acceptRemoteIdentity(
            peerId: String,
            encryptionPublicKey: ByteArray,
            signingPublicKey: ByteArray
        ): Result<Unit> = error(NOT_USED)

        override suspend fun establishMutualIdentity(
            peerId: String,
            encryptionPublicKey: ByteArray,
            signingPublicKey: ByteArray
        ): Result<Unit> = error(NOT_USED)

        override suspend fun ensureRemoteSigningIdentity(peerId: String, signingPublicKey: ByteArray): Result<Unit> =
            error(NOT_USED)

        override suspend fun start(peerId: String, invitationSenderLabel: String): Result<IdentityExchange?> =
            error(NOT_USED)

        override suspend fun startManual(peerId: String): Result<Unit> = error(NOT_USED)

        override suspend fun accept(exchangeId: String): Result<Unit> = error(NOT_USED)

        override suspend fun decline(exchangeId: String): Result<Unit> = error(NOT_USED)

        override fun observeState(peerId: String): Flow<IdentityHandshakeState?> = error(NOT_USED)

        override fun observeResults(): Flow<List<IdentityResult>> = error(NOT_USED)

        override suspend fun cancel(peerId: String): Result<Unit> = error(NOT_USED)

        override suspend fun getExchangeClosure(peerId: String): Result<IdentityExchangeClosure?> = error(NOT_USED)

        override suspend fun closeExchange(exchangeId: String, peerId: String): Result<Unit> = error(NOT_USED)

        override suspend fun getExchangeBinding(exchangeId: String): Result<IdentityExchangeBinding?> =
            error(NOT_USED)

        override suspend fun invalidateExchange(
            exchangeId: String,
            peerId: String,
            expectedChallenge: ByteArray,
            expectedSigningPublicKey: ByteArray,
            atEpochMilliseconds: Long
        ): Result<Unit> = error(NOT_USED)

        override suspend fun receiveExchange(
            context: IncomingPacketContext,
            offer: IdentityExchangeOffer,
            wasKnownPeerAtReceive: Boolean
        ): Result<Unit> = error(NOT_USED)

        override suspend fun acceptApprovedIncomingExchange(
            context: IncomingPacketContext,
            offer: IdentityExchangeOffer,
            wasKnownPeerAtReceive: Boolean
        ): Result<Unit> = error(NOT_USED)

        override suspend fun reassignPeer(fromPeerId: String, toPeerId: String): Result<Unit> = error(NOT_USED)

        override suspend fun receiveAccepted(
            context: IncomingPacketContext,
            acceptance: IdentityExchangeAcceptance
        ): Result<Unit> = error(NOT_USED)

        override suspend fun recordRemoteDecline(
            exchangeId: String,
            peerId: String,
            inviteChallenge: ByteArray,
            remoteSigningPublicKey: ByteArray,
            receivedAtEpochMilliseconds: Long
        ): Result<Unit> = error(NOT_USED)

        override suspend fun receiveReady(context: IncomingPacketContext, ready: IdentityExchangeReady): Result<Unit> =
            error(NOT_USED)

        override suspend fun receiveManualIdentity(
            context: IncomingPacketContext,
            packet: IdentityPacket
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun receiveIdentityAcknowledgement(
            context: IncomingPacketContext,
            packet: IdentityAcknowledgementPacket
        ): Result<Boolean> = error(NOT_USED)
    }

    private class NoPendingChanges : PendingRemoteIdentityChangeRepository {
        override fun observeAll(): Flow<List<PendingRemoteIdentityChange>> = flowOf(emptyList())

        override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> = error(NOT_USED)

        override suspend fun discard(peerId: String, invitationId: String): Result<Unit> = error(NOT_USED)

        override suspend fun approveReplacement(
            peerId: String,
            invitationId: String,
            presentedSigningFingerprint: String,
            presentedEncryptionFingerprint: String
        ): Result<Unit> = error(NOT_USED)
    }

    private class FakeBlocklist : ContactBlocklistRepository {
        val blocked = mutableSetOf<String>()

        override suspend fun isBlocked(contactId: String): Result<Boolean> = Result.success(contactId in blocked)

        override fun observeBlockUnknownContactInvites(): Flow<Boolean> = error(NOT_USED)

        override suspend fun getBlockUnknownContactInvites(): Result<Boolean> = error(NOT_USED)

        override suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit> = error(NOT_USED)

        override fun observeBlockedContactIds(): Flow<Set<String>> = error(NOT_USED)

        override suspend fun getBlockedContactIds(): Result<Set<String>> = error(NOT_USED)

        override suspend fun block(contactId: String): Result<Unit> = error(NOT_USED)

        override suspend fun unblock(contactId: String): Result<Unit> = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used by the parked envelope tests"
        const val GROUP_ID = "group"
        const val CONTACT_ID = "alice"
        const val ROUTING_ID = "route"
        const val PAYLOAD = "payload"
    }
}
