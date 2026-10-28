package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.mailbox

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.feature.contacts.domain.model.MailboxContactFacts
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactBlocklistRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactTransportRepository
import com.cbgm.sparrow.feature.contacts.domain.usecase.GetMailboxContactStatesUseCase
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
import com.cbgm.sparrow.feature.messaging.runtime.mailbox.MailboxRoutePayloadEncoder
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpoint
import com.cbgm.sparrow.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.sparrow.feature.transport.gateway.model.FederatedEnvelope
import com.cbgm.sparrow.feature.transport.mailbox.MailboxGateway
import com.cbgm.sparrow.feature.transport.routing.LocalRoutingIdProvider
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.mailbox.LocalMailboxCredential
import com.cbgm.sparrow.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.sparrow.protocol.mailbox.MailboxRouteRepository
import com.cbgm.sparrow.protocol.mailbox.NoOpMailboxCapabilityLifecycle
import com.cbgm.sparrow.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxFailureEvent
import com.cbgm.sparrow.protocol.outbox.ProtocolOutboxItem
import com.cbgm.sparrow.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.IdentityPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val NOT_USED = "Not used"
internal const val CONTACT_ID = "contact-1"
internal const val OTHER_CONTACT_ID = "contact-2"
private const val NODE_ID = "node-a"
private const val NODE_ENDPOINT = "http://node-a/route"
private const val ACCESS_ENDPOINT = "http://node-a/access"

internal fun mailboxCredential(
    contactId: String,
    name: String,
    expiresAt: Long,
    nodeId: String = NODE_ID
) = LocalMailboxCredential(
    contactId = contactId,
    deliveryRoute =
        MailboxDeliveryRoute(
            routeId = "route-$name",
            nodeId = nodeId,
            nodeEndpoint = NODE_ENDPOINT,
            mailboxId = "mailbox-$name",
            sendCapability = "send-$name",
            sequence = 1,
            expiresAtEpochMilliseconds = expiresAt,
            identitySignature = byteArrayOf(1)
        ),
    accessEndpoint = ACCESS_ENDPOINT,
    retrievalCapability = "retrieve-$name"
)

internal fun facts(
    contactId: String,
    hasDirectConversation: Boolean = false,
    sharesActiveGroup: Boolean = false
) = MailboxContactFacts(
    contactId = contactId,
    hasMutualIdentity = true,
    hasRoutingId = true,
    hasDirectConversation = hasDirectConversation,
    sharesActiveGroup = sharesActiveGroup
)

/** Everything [MailboxRouteProvisioner] talks to, kept in memory. */
internal class ProvisionerFakes {
    val routes = InMemoryMailboxRoutes()
    val gateway = RecordingMailboxGateway()
    val contacts = FakeContactTransport()
    val identity = FakeIdentityExchanges()
    val outbox = RecordingOutbox()

    fun provisioner() =
        MailboxRouteProvisioner(
            getMailboxContactStates =
                GetMailboxContactStatesUseCase(contacts, GetIdentityPeerStateUseCase(identity, NoPendingChanges)),
            localRoutingIdProvider = FakeLocalRoutingId,
            nodeEndpointResolver = SingleNodeResolver,
            mailboxGateway = gateway,
            mailboxRouteRepository = routes,
            mailboxCapabilityLifecycle = NoOpMailboxCapabilityLifecycle,
            contactBlocklistRepository = NoBlockedContacts,
            credentialFactory =
                MailboxCredentialFactory(gateway, FakeSigningKeys, FakeSignatures, MailboxRoutePayloadEncoder()),
            protocolOutbox = outbox
        )
}

internal class InMemoryMailboxRoutes : MailboxRouteRepository {
    val local = linkedMapOf<String, LocalMailboxCredential>()
    val retiring = linkedMapOf<String, LocalMailboxCredential>()

    override suspend fun localForContact(contactId: String) = Result.success(local[contactId])

    override suspend fun remoteForRecipientRoutingId(routingId: String): Result<MailboxDeliveryRoute?> =
        error(NOT_USED)

    override suspend fun allLocal() = Result.success(local.values.toList())

    override suspend fun saveLocal(credential: LocalMailboxCredential): Result<Unit> {
        local[credential.contactId] = credential
        return Result.success(Unit)
    }

    override suspend fun saveRemote(contactId: String, route: MailboxDeliveryRoute): Result<Unit> = error(NOT_USED)

    override suspend fun markLocalRevocationPending(contactId: String): Result<Unit> = error(NOT_USED)

    override suspend fun deleteLocal(contactId: String): Result<Unit> {
        local.remove(contactId)
        return Result.success(Unit)
    }

    override suspend fun deleteRemote(contactId: String): Result<Unit> = error(NOT_USED)

    override suspend fun deleteAllRemote(): Result<Unit> = error(NOT_USED)

    override suspend fun allRetiring() = Result.success(retiring.values.toList())

    override suspend fun retireLocal(
        retiring: LocalMailboxCredential,
        replacement: LocalMailboxCredential
    ): Result<Unit> {
        this.retiring[retiring.deliveryRoute.routeId] = retiring
        local[replacement.contactId] = replacement
        return Result.success(Unit)
    }

    override suspend fun saveRetiring(credential: LocalMailboxCredential): Result<Unit> {
        retiring[credential.deliveryRoute.routeId] = credential
        return Result.success(Unit)
    }

    override suspend fun markRetiringRevocationPending(contactId: String): Result<Unit> = error(NOT_USED)

    override suspend fun deleteRetiring(routeId: String): Result<Unit> {
        retiring.remove(routeId)
        return Result.success(Unit)
    }
}

/** Creates mailboxes on node-a; routes in [pendingRoutes] still hold envelopes. */
internal class RecordingMailboxGateway : MailboxGateway {
    val created = mutableListOf<String>()
    val revoked = mutableListOf<String>()
    val pendingRoutes = mutableSetOf<String>()

    override suspend fun create(
        contactId: String,
        nodeId: String,
        routeEndpoint: String,
        accessEndpoint: String,
        sequence: Long,
        expiresAtEpochMilliseconds: Long
    ): Result<LocalMailboxCredential> {
        created += contactId
        return Result.success(
            mailboxCredential(contactId, "new-${created.size}", expiresAtEpochMilliseconds, nodeId)
                .let { credential -> credential.copy(deliveryRoute = credential.deliveryRoute.copy(sequence = sequence)) }
        )
    }

    override suspend fun pending(credential: LocalMailboxCredential): Result<List<FederatedEnvelope>> =
        Result.success(
            if (credential.deliveryRoute.routeId in pendingRoutes) {
                listOf(
                    FederatedEnvelope(
                        envelopeId = "envelope",
                        senderRoutingId = "sender",
                        recipientDeviceRoutingId = "recipient",
                        encryptedPayload = "payload",
                        createdAtEpochMilliseconds = 0,
                        expiresAtEpochMilliseconds = Long.MAX_VALUE
                    )
                )
            } else {
                emptyList()
            }
        )

    override suspend fun acknowledge(credential: LocalMailboxCredential, envelopeId: String): Result<Unit> =
        error(NOT_USED)

    override suspend fun revoke(credential: LocalMailboxCredential): Result<Unit> {
        revoked += credential.deliveryRoute.routeId
        return Result.success(Unit)
    }
}

internal class FakeContactTransport : ContactTransportRepository {
    var facts: List<MailboxContactFacts> = emptyList()

    override suspend fun getMailboxContacts(): Result<List<MailboxContactFacts>> = Result.success(facts)

    override suspend fun resolveRoutingId(contactId: String): Result<String> = error(NOT_USED)

    override suspend fun resolveBootstrapRoutingId(contactId: String): Result<String> = error(NOT_USED)

    override suspend fun resolveInvitationRoutingId(contactId: String): Result<String> = error(NOT_USED)

    override suspend fun resolveContactIdByRoutingId(routingId: String): Result<String?> = error(NOT_USED)

    override suspend fun reconcileKnownContacts(): Result<Unit> = error(NOT_USED)

    override suspend fun getMutualSigningPublicKey(contactId: String): Result<ByteArray?> = error(NOT_USED)
}

/** Only the peer state is read: peers in [establishedPeers] have an established exchange. */
internal class FakeIdentityExchanges : IdentityExchangeRepository {
    val establishedPeers = mutableSetOf<String>()

    override suspend fun getPeerState(peerId: String): Result<IdentityPeerState> =
        Result.success(IdentityPeerState(hasEstablishedExchange = peerId in establishedPeers, hasMutualIdentity = true))

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

    override suspend fun getExchangeBinding(exchangeId: String): Result<IdentityExchangeBinding?> = error(NOT_USED)

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

    override suspend fun receiveManualIdentity(context: IncomingPacketContext, packet: IdentityPacket): Result<Boolean> =
        error(NOT_USED)

    override suspend fun receiveIdentityAcknowledgement(
        context: IncomingPacketContext,
        packet: IdentityAcknowledgementPacket
    ): Result<Boolean> = error(NOT_USED)
}

internal class RecordingOutbox : ProtocolOutbox {
    val enqueued = mutableListOf<Pair<String, SparrowPacket>>()

    override suspend fun enqueue(contactId: String, packet: SparrowPacket): Result<ProtocolOutboxItem> {
        enqueued += contactId to packet
        return Result.success(
            ProtocolOutboxItem(
                id = "item-${enqueued.size}",
                contactId = contactId,
                packetId = packet.packetId,
                encodedPacket = byteArrayOf(1),
                status = OutboxStatus.PENDING,
                attemptCount = 0,
                lastError = null,
                createdAtEpochMilliseconds = 1L,
                updatedAtEpochMilliseconds = 1L
            )
        )
    }

    override fun observePending(): Flow<List<ProtocolOutboxItem>> = error(NOT_USED)

    override fun observeTransportStates(): Flow<List<ProtocolOutboxItem>> = error(NOT_USED)

    override fun observeUnacknowledgedFailures(): Flow<List<ProtocolOutboxFailureEvent>> = error(NOT_USED)

    override suspend fun acknowledgeFailure(eventId: String): Result<Unit> = error(NOT_USED)

    override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = error(NOT_USED)

    override suspend fun markProcessing(itemId: String): Result<Unit> = error(NOT_USED)

    override suspend fun markSent(itemId: String): Result<Unit> = error(NOT_USED)

    override suspend fun markFailed(itemId: String, errorMessage: String): Result<Unit> = error(NOT_USED)

    override suspend fun retry(itemId: String): Result<Unit> = error(NOT_USED)

    override suspend fun resend(packetId: String): Result<Unit> = error(NOT_USED)

    override suspend fun requeueInterrupted(): Result<Unit> = error(NOT_USED)

    override suspend fun retryFailed(): Result<Unit> = error(NOT_USED)

    override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = error(NOT_USED)
}

private object NoPendingChanges : PendingRemoteIdentityChangeRepository {
    override suspend fun stage(candidate: PendingRemoteIdentityChange): Result<Unit> = error(NOT_USED)

    override fun observeAll(): Flow<List<PendingRemoteIdentityChange>> = flowOf(emptyList())

    override suspend fun discard(peerId: String, invitationId: String): Result<Unit> = error(NOT_USED)

    override suspend fun approveReplacement(
        peerId: String,
        invitationId: String,
        presentedSigningFingerprint: String,
        presentedEncryptionFingerprint: String
    ): Result<Unit> = error(NOT_USED)
}

private object FakeLocalRoutingId : LocalRoutingIdProvider {
    override suspend fun getLocalRoutingId(): Result<String> = Result.success("local-routing-id")
}

private object SingleNodeResolver : NodeEndpointResolver {
    override suspend fun resolve(localRoutingId: String, forceRefresh: Boolean): Result<List<NodeEndpoint>> =
        Result.success(
            listOf(
                NodeEndpoint(
                    nodeId = NODE_ID,
                    websocketUrl = "ws://node-a",
                    mailboxRouteEndpoint = NODE_ENDPOINT,
                    mailboxAccessEndpoint = ACCESS_ENDPOINT
                )
            )
        )
}

private object NoBlockedContacts : ContactBlocklistRepository {
    override fun observeBlockUnknownContactInvites(): Flow<Boolean> = error(NOT_USED)

    override suspend fun getBlockUnknownContactInvites(): Result<Boolean> = error(NOT_USED)

    override suspend fun setBlockUnknownContactInvites(enabled: Boolean): Result<Unit> = error(NOT_USED)

    override fun observeBlockedContactIds(): Flow<Set<String>> = error(NOT_USED)

    override suspend fun getBlockedContactIds(): Result<Set<String>> = Result.success(emptySet())

    override suspend fun isBlocked(contactId: String): Result<Boolean> = error(NOT_USED)

    override suspend fun block(contactId: String): Result<Unit> = error(NOT_USED)

    override suspend fun unblock(contactId: String): Result<Unit> = error(NOT_USED)
}

private object FakeSigningKeys : LocalSigningKeyPairProvider {
    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
        Result.success(LocalSigningKeyPair(publicKey = ByteArray(32), privateKey = ByteArray(64)))
}

private object FakeSignatures : DetachedSignatureCrypto {
    override suspend fun sign(payload: ByteArray, signingPrivateKey: ByteArray): Result<ByteArray> =
        Result.success(byteArrayOf(9))

    override suspend fun verify(payload: ByteArray, signingPublicKey: ByteArray, signature: ByteArray): Result<Unit> =
        error(NOT_USED)
}
