package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationDeclineProtocol
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationHandshakeProtocol
import com.cbgm.sparrow.core.protocol.invitation.ContactInvitationRequestProtocol
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.IdentityExchangeEntity
import com.cbgm.sparrow.feature.identity.data.datasource.IdentityExchangeStoreDataSource
import com.cbgm.sparrow.feature.identity.data.model.IdentityExchangeBindingDto
import com.cbgm.sparrow.feature.identity.data.model.IdentityExchangeStage
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosurePhase
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeDirection
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResult
import com.cbgm.sparrow.feature.identity.domain.model.IdentityResultStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class IdentityExchangeDataSource(
    private val store: IdentityExchangeStoreDataSource,
    private val remoteIdentityDataSource: RemoteIdentityDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val secureRandomGenerator: SecureRandomGenerator,
    private val invitationRequestProtocol: ContactInvitationRequestProtocol,
    private val invitationHandshakeProtocol: ContactInvitationHandshakeProtocol,
    private val invitationDeclineProtocol: ContactInvitationDeclineProtocol,
    private val protocolOutbox: ProtocolOutbox,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider,
    private val identitySetupModeRepository: DirectIdentitySetupModeRepository
) {
    private val logger = SparrowLog.withTag("IdentityExchangeDataSource")

    private val mutex = Mutex()

    // Invitation challenge/response authenticates chat acceptance in both modes.
    // It is not a manual identity import or acknowledgement: only the explicit
    // IdentityPacket/IdentityAcknowledgementPacket flow may establish manual keys.
    private suspend fun canImportIdentityFromInvitation(): Boolean =
        identitySetupModeRepository.getMode() == DirectIdentitySetupMode.AUTOMATIC_INVITATION

    suspend fun start(contactId: String, invitationSenderLabel: String): Result<IdentityExchange?> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            mutex.withLock {
                val remoteIdentity = remoteIdentityDataSource.findByPeerId(contactId)
                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)

                if (hasEstablishedIdentityExchange(contactId, localIdentity)) {
                    return@withLock null
                }

                val now = SystemClock.nowEpochMilliseconds()
                store.findActiveForContact(contactId, TERMINAL_STATES)?.let { activeInvitation ->
                    if (!isBoundToLocalIdentity(activeInvitation, localIdentity)) {
                        store.upsert(
                            activeInvitation.copy(
                                stage = IdentityExchangeStage.FAILED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Handshake belongs to a previous local identity"
                            )
                        )
                    } else if (activeInvitation.expiresAtEpochMilliseconds > now) {
                        if (resumeActiveHandshake(activeInvitation)) {
                            return@withLock activeInvitation
                                .takeIf { invitation ->
                                    invitation.direction == IdentityExchangeDirection.OUTGOING.name &&
                                        invitation.stage == IdentityExchangeStage.OUTGOING_CHALLENGE_SENT.name
                                }?.toLifecycleRecord()
                        }

                        store.upsert(
                            activeInvitation.copy(
                                stage = IdentityExchangeStage.FAILED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Handshake was superseded by a fresh invitation"
                            )
                        )
                    } else {
                        store.upsert(
                            activeInvitation.copy(
                                stage = IdentityExchangeStage.CLOSED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Invitation expired"
                            )
                        )
                    }
                }

                val invitationId = IdGenerator.generate()
                val challenge = secureRandomGenerator.generateBytes(CHALLENGE_SIZE).getOrThrow()
                val expiresAt = now + INVITATION_LIFETIME_MILLISECONDS
                val profilePicture = localProfilePictureMetadataProvider.forInvite().getOrElse { ProfilePictureMetadata() }
                val packet = invitationRequestProtocol.createPacket(
                    invitationId = invitationId,
                    displayName = invitationSenderLabel,
                    createdAtEpochMilliseconds = now,
                    expiresAtEpochMilliseconds = expiresAt,
                    profilePicture = profilePicture,
                    inviteChallenge = challenge,
                    encryptionPublicKey = localIdentity.encryptionPublicKey,
                    signingKeyPair = signingKeyPair
                ).getOrThrow()

                val storedInvitation =
                    IdentityExchangeEntity(
                        exchangeId = invitationId,
                        contactId = contactId,
                        direction = IdentityExchangeDirection.OUTGOING.name,
                        stage = IdentityExchangeStage.OUTGOING_CHALLENGE_SENT.name,
                        // Local contact labels belong to Contacts/Invite, not Identity.
                        remoteDisplayName = null,
                        inviteChallenge = challenge.copyOf(),
                        responseChallenge = null,
                        remoteEncryptionPublicKey =
                            remoteIdentity?.encryptionPublicKey?.copyOf() ?: byteArrayOf(),
                        remoteSigningPublicKey = remoteIdentity?.signingPublicKey?.copyOf() ?: byteArrayOf(),
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        updatedAtEpochMilliseconds = now,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                store.upsert(storedInvitation)

                enqueueOrResend(contactId, packet).getOrElse { error ->
                    store.upsert(
                        requireNotNull(store.findById(invitationId)).copy(
                            stage = IdentityExchangeStage.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }

                storedInvitation.toLifecycleRecord()
            }
        }

    suspend fun startManual(peerId: String): Result<Unit> =
        safeSuspendCall {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            protocolOutbox.enqueue(
                contactId = peerId,
                packet = IdentityPacket(
                    packetId = IdGenerator.generate(),
                    displayName = null,
                    encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                    signingPublicKey = localIdentity.signingPublicKey.copyOf()
                )
            ).getOrThrow()
        }

    fun observeResults(): Flow<List<IdentityResult>> =
        store
            .observeAll()
            .map { exchanges -> exchanges.mapNotNull(::toResult) }

    fun observeState(contactId: String): Flow<IdentityHandshakeState?> {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        return combine(
            store.observeLatestForContact(contactId),
            store.observeLatestForContactByStages(
                contactId = contactId,
                stages = EXCHANGE_EVENT_STAGES
            )
        ) { latestInvitation, latestAuthorizationEvent ->
            val state =
                resolveObservedState(
                    latestInvitation = latestInvitation,
                    latestAuthorizationEvent = latestAuthorizationEvent
                )
            if (state !in ESTABLISHED_EXCHANGE_STATES) {
                return@combine state
            }

            val authorizationEvent =
                when (state) {
                    IdentityHandshakeState.WAITING_FOR_READY -> latestInvitation
                    IdentityHandshakeState.MUTUAL_UNVERIFIED -> latestAuthorizationEvent
                    else -> null
                }
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrNull()
            if (
                authorizationEvent != null &&
                localIdentity != null &&
                isBoundToLocalIdentity(authorizationEvent, localIdentity)
            ) {
                state
            } else {
                null
            }
        }
    }

    suspend fun accept(invitationId: String): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                val invitation = requireInvitation(invitationId, IdentityExchangeDirection.INCOMING)
                ensureNotExpired(invitation)

                if (
                    invitation.stage == IdentityExchangeStage.ACCEPTANCE_SENT.name ||
                    invitation.stage == IdentityExchangeStage.WAITING_FOR_READY.name
                ) {
                    queueAcceptanceReplay(invitation)
                    return@withLock
                }
                if (invitation.stage == IdentityExchangeStage.MUTUAL_UNVERIFIED.name) {
                    return@withLock
                }

                requireState(invitation, IdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED)

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)

                val now = SystemClock.nowEpochMilliseconds()
                val profilePicture = localProfilePictureMetadataProvider.forInvite().getOrElse { ProfilePictureMetadata() }
                val responseChallenge = secureRandomGenerator.generateBytes(CHALLENGE_SIZE).getOrThrow()
                val packet = invitationHandshakeProtocol.createAccepted(
                    invitationId = invitationId,
                    acceptedAtEpochMilliseconds = now,
                    profilePicture = profilePicture,
                    inviteChallenge = invitation.inviteChallenge,
                    responseChallenge = responseChallenge,
                    inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                    inviterSigningPublicKey = invitation.remoteSigningPublicKey,
                    responderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                    signingKeyPair = signingKeyPair
                ).getOrThrow()

                if (canImportIdentityFromInvitation()) prepareAcceptedRemoteIdentity(invitation)
                store.upsert(
                    invitation.copy(
                        stage = IdentityExchangeStage.ACCEPTANCE_SENT.name,
                        responseChallenge = responseChallenge.copyOf(),
                        updatedAtEpochMilliseconds = now,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )
                enqueueOrResend(invitation.contactId, packet).getOrElse { error ->
                    store.upsert(
                        requireNotNull(store.findById(invitationId)).copy(
                            stage = IdentityExchangeStage.ACCEPTANCE_SENT.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
                if (canImportIdentityFromInvitation()) {
                    remoteIdentityDataSource.markMutual(
                        peerId = invitation.contactId,
                        encryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        signingPublicKey = invitation.remoteSigningPublicKey
                    )
                }
                store.upsert(
                    requireNotNull(store.findById(invitationId)).copy(
                        stage = IdentityExchangeStage.WAITING_FOR_READY.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            }
        }

    suspend fun decline(invitationId: String): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                val invitation = requireInvitation(invitationId, IdentityExchangeDirection.INCOMING)
                if (invitation.stage == IdentityExchangeStage.CLOSED.name) {
                    resendPersistedPacket(declinedPacketId(invitation.exchangeId))
                    return@withLock
                }
                // A previously displayed invitation can be declined after its exchange
                // has already been revoked. Never send an obsolete decline or overwrite
                // the revocation with CLOSED; the revoked exchange remains terminal.
                if (invitation.stage == IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue) {
                    return@withLock
                }
                requireState(invitation, IdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED)

                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val now = SystemClock.nowEpochMilliseconds()
                val packet = invitationDeclineProtocol.createPacket(
                    exchangeId = invitationId,
                    inviteChallenge = invitation.inviteChallenge,
                    declinedAtEpochMilliseconds = now,
                    signingKeyPair = signingKeyPair
                ).getOrThrow()

                enqueueOrResend(invitation.contactId, packet).getOrElse { error ->
                    store.upsert(
                        invitation.copy(
                            stage = IdentityExchangeStage.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
                store.upsert(
                    invitation.copy(
                        stage = IdentityExchangeStage.CLOSED.name,
                        updatedAtEpochMilliseconds = now,
                        lastError = null
                    )
                )
            }
        }

    suspend fun cancel(contactId: String): Result<Unit> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            mutex.withLock {
                val invitation =
                    store.findActiveForContact(
                        contactId = contactId,
                        terminalStages = TERMINAL_STATES
                    ) ?: return@withLock

                val state =
                    IdentityExchangeStage.entries.firstOrNull { candidate ->
                        candidate.persistedValue == invitation.stage
                    } ?: return@withLock

                when {
                    invitation.direction == IdentityExchangeDirection.INCOMING.name &&
                        state == IdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED -> {
                        queueDecline(
                            contactId = invitation.contactId,
                            invitationId = invitation.exchangeId,
                            inviteChallenge = invitation.inviteChallenge
                        )

                        store.upsert(
                            invitation.copy(
                                stage = IdentityExchangeStage.CLOSED.name,
                                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                                lastError = "Manual identity exchange selected"
                            )
                        )
                    }

                    invitation.direction == IdentityExchangeDirection.OUTGOING.name &&
                        state == IdentityExchangeStage.OUTGOING_CHALLENGE_SENT -> {
                        store.upsert(
                            invitation.copy(
                                stage = IdentityExchangeStage.CLOSED.name,
                                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                                lastError = "Manual identity exchange selected"
                            )
                        )
                    }
                }
            }
        }

    suspend fun getPeerState(contactId: String): Result<IdentityPeerState> =
        safeSuspendCall {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val hasEstablishedExchange = hasEstablishedIdentityExchange(contactId, localIdentity)
            val peerIdentity = remoteIdentityDataSource.findByPeerId(contactId)
            val hasMutualIdentity =
                peerIdentity?.locallyImported == true &&
                    peerIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name
            IdentityPeerState(
                hasEstablishedExchange = hasEstablishedExchange,
                hasMutualIdentity = hasMutualIdentity
            )
        }

    /** Snapshot only: no cross-feature decision, packet enqueue, or state mutation. */
    suspend fun getExchangeClosure(peerId: String): Result<IdentityExchangeClosure?> =
        safeSuspendCall {
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            mutex.withLock {
                store.findLatestForContact(peerId)?.let { exchange ->
                    val stage = IdentityExchangeStage.entries.firstOrNull { it.persistedValue == exchange.stage }
                        ?: error("Unknown identity exchange state: ${exchange.stage}")
                    val phase = when {
                        stage == IdentityExchangeStage.EXCHANGE_INVALIDATED ->
                            IdentityExchangeClosurePhase.ALREADY_CLOSED
                        stage == IdentityExchangeStage.CLOSED || stage == IdentityExchangeStage.FAILED ->
                            IdentityExchangeClosurePhase.TERMINAL
                        exchange.direction == IdentityExchangeDirection.INCOMING.name &&
                            stage == IdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED ->
                            IdentityExchangeClosurePhase.INCOMING_PENDING
                        else -> IdentityExchangeClosurePhase.ACTIVE
                    }
                    IdentityExchangeClosure(
                        exchangeId = exchange.exchangeId,
                        peerId = exchange.contactId,
                        phase = phase,
                        inviteChallenge = exchange.inviteChallenge
                    )
                }
            }
        }

    /** Apply only to the exchange that was prepared; never close a newer exchange by accident. */
    suspend fun closeExchange(exchangeId: String, peerId: String): Result<Unit> =
        safeSuspendCall {
            require(exchangeId.isNotBlank()) { "Exchange ID must not be blank" }
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            mutex.withLock {
                val latest = store.findLatestForContact(peerId)
                    ?: error("Identity exchange was not found for peer: $peerId")
                check(latest.exchangeId == exchangeId) { "A newer identity exchange superseded $exchangeId" }
                if (latest.stage != IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue) {
                    store.upsert(
                        latest.copy(
                            stage = IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = null
                        )
                    )
                }
            }
        }

    /** Rebind pending exchanges when orchestration merges two peer records. */
    suspend fun reassignPeer(fromPeerId: String, toPeerId: String): Result<Unit> =
        safeSuspendCall {
            require(fromPeerId.isNotBlank() && toPeerId.isNotBlank()) {
                "Peer IDs must not be blank"
            }
            if (fromPeerId != toPeerId) {
                mutex.withLock {
                    store.reassignContact(fromPeerId, toPeerId)
                }
            }
        }

    suspend fun receiveExchange(
        context: IncomingPacketContext,
        offer: IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                // The packet signature, packet ID and lifetime were verified by
                // ContactInvitationRequestProtocol in orchestration before Contacts reconciliation.
                val contactId = context.contactId
                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

                store.findById(offer.exchangeId)?.let { existing ->
                    check(existing.direction == IdentityExchangeDirection.INCOMING.name) {
                        "Invitation replay changed its direction"
                    }
                    check(existing.contactId == contactId) {
                        "Invitation replay used a different contact"
                    }
                    check(existing.createdAtEpochMilliseconds == offer.createdAtEpochMilliseconds) {
                        "Invitation replay changed its creation time"
                    }
                    check(existing.expiresAtEpochMilliseconds == offer.expiresAtEpochMilliseconds) {
                        "Invitation replay changed its expiry time"
                    }
                    check(existing.inviteChallenge.contentEquals(offer.inviteChallenge)) {
                        "Invitation replay changed its challenge"
                    }
                    check(existing.remoteEncryptionPublicKey.contentEquals(offer.encryptionPublicKey)) {
                        "Invitation replay changed its encryption key"
                    }
                    check(existing.remoteSigningPublicKey.contentEquals(offer.signingPublicKey)) {
                        "Invitation replay changed its signing key"
                    }
                    val reboundExisting =
                        existing.copy(
                            localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                            localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                        )
                    store.upsert(reboundExisting)
                    recoverIncomingInviteReplay(reboundExisting)
                    return@withLock
                }

                store
                    .findActiveForContact(
                        contactId = contactId,
                        terminalStages = TERMINAL_STATES
                    )?.let { activeInvitation ->
                        if (activeInvitation.expiresAtEpochMilliseconds <= context.receivedAtEpochMilliseconds) {
                            store.upsert(
                                activeInvitation.copy(
                                    stage = IdentityExchangeStage.CLOSED.name,
                                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                                    lastError = "Invitation expired"
                                )
                            )
                        } else {
                            check(
                                activeInvitation.remoteEncryptionPublicKey.isEmpty() ||
                                    activeInvitation.remoteEncryptionPublicKey.contentEquals(
                                        offer.encryptionPublicKey
                                    )
                            ) {
                                "Another active contact invitation pins a different encryption key"
                            }
                            check(
                                activeInvitation.remoteSigningPublicKey.isEmpty() ||
                                    activeInvitation.remoteSigningPublicKey.contentEquals(
                                        offer.signingPublicKey
                                    )
                            ) {
                                "Another active contact invitation pins a different signing key"
                            }
                        }
                    }

                if (canImportIdentityFromInvitation()) {
                    stageIncomingInvitationIdentity(
                        contactId = contactId,
                        remoteEncryptionPublicKey = offer.encryptionPublicKey,
                        remoteSigningPublicKey = offer.signingPublicKey
                    )
                }

                store.upsert(
                    IdentityExchangeEntity(
                        exchangeId = offer.exchangeId,
                        contactId = contactId,
                        direction = IdentityExchangeDirection.INCOMING.name,
                        stage = IdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED.name,
                        remoteDisplayName = null,
                        inviteChallenge = offer.inviteChallenge.copyOf(),
                        responseChallenge = null,
                        remoteEncryptionPublicKey = offer.encryptionPublicKey.copyOf(),
                        remoteSigningPublicKey = offer.signingPublicKey.copyOf(),
                        createdAtEpochMilliseconds = offer.createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = offer.expiresAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                        wasKnownPeerAtReceive = wasKnownPeerAtReceive
                    )
                )
            }
        }

    suspend fun receiveAccepted(
        context: IncomingPacketContext,
        acceptance: IdentityExchangeAcceptance
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                val invitation = requireInvitation(acceptance.exchangeId, IdentityExchangeDirection.OUTGOING)
                check(invitation.contactId == context.contactId) {
                    "Acceptance contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(acceptance.inviteChallenge)) {
                    "Acceptance challenge does not match invitation"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(localIdentity.encryptionPublicKey.contentEquals(acceptance.inviterEncryptionPublicKey)) {
                    "Acceptance refers to a different local encryption key"
                }
                check(localIdentity.signingPublicKey.contentEquals(acceptance.inviterSigningPublicKey)) {
                    "Acceptance refers to a different local signing key"
                }

                require(acceptance.acceptedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                    "Acceptance was created after the invitation expired"
                }
                check(
                    invitation.remoteEncryptionPublicKey.isEmpty() ||
                        invitation.remoteEncryptionPublicKey.contentEquals(
                            acceptance.responderEncryptionPublicKey
                        )
                ) {
                    "Contact encryption identity changed during invitation acceptance"
                }
                check(
                    invitation.remoteSigningPublicKey.isEmpty() ||
                        invitation.remoteSigningPublicKey.contentEquals(
                            acceptance.responderSigningPublicKey
                        )
                ) {
                    "Contact signing identity changed during invitation acceptance"
                }

                if (invitation.stage == IdentityExchangeStage.MUTUAL_UNVERIFIED.name) {
                    check(invitation.responseChallenge?.contentEquals(acceptance.responseChallenge) == true) {
                        "Acceptance replay changed its response challenge"
                    }
                    check(invitation.remoteEncryptionPublicKey.contentEquals(acceptance.responderEncryptionPublicKey)) {
                        "Acceptance replay changed its encryption key"
                    }
                    check(invitation.remoteSigningPublicKey.contentEquals(acceptance.responderSigningPublicKey)) {
                        "Acceptance replay changed its signing key"
                    }
                    queueReadyReplay(
                        contactId = context.contactId,
                        acceptance = acceptance
                    )
                    return@withLock
                }

                requireState(invitation, IdentityExchangeStage.OUTGOING_CHALLENGE_SENT)

                if (canImportIdentityFromInvitation()) {
                    remoteIdentityDataSource.stage(
                        peerId = context.contactId,
                        encryptionPublicKey = acceptance.responderEncryptionPublicKey,
                        signingPublicKey = acceptance.responderSigningPublicKey
                    )
                    remoteIdentityDataSource.accept(
                        peerId = context.contactId,
                        encryptionPublicKey = acceptance.responderEncryptionPublicKey,
                        signingPublicKey = acceptance.responderSigningPublicKey
                    )
                }
                val now = SystemClock.nowEpochMilliseconds()
                queueReadyReplay(
                    contactId = context.contactId,
                    acceptance = acceptance
                )
                if (canImportIdentityFromInvitation()) {
                    remoteIdentityDataSource.markMutual(
                        peerId = context.contactId,
                        encryptionPublicKey = acceptance.responderEncryptionPublicKey,
                        signingPublicKey = acceptance.responderSigningPublicKey
                    )
                }

                store.upsert(
                    invitation.copy(
                        stage = IdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                        responseChallenge = acceptance.responseChallenge.copyOf(),
                        remoteEncryptionPublicKey = acceptance.responderEncryptionPublicKey.copyOf(),
                        remoteSigningPublicKey = acceptance.responderSigningPublicKey.copyOf(),
                        updatedAtEpochMilliseconds = now,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )
            }
        }

    suspend fun receiveReady(
        context: IncomingPacketContext,
        ready: IdentityExchangeReady
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                    "ContactReadyPacket must be received through encrypted transport"
                }

                val invitation = requireInvitation(ready.exchangeId, IdentityExchangeDirection.INCOMING)
                check(invitation.contactId == context.contactId) {
                    "Ready peer does not match identity exchange"
                }
                check(invitation.responseChallenge?.contentEquals(ready.responseChallenge) == true) {
                    "Ready challenge does not match acceptance"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(localIdentity.encryptionPublicKey.contentEquals(ready.acceptedResponderEncryptionPublicKey)) {
                    "Ready packet refers to a different local encryption key"
                }
                check(localIdentity.signingPublicKey.contentEquals(ready.acceptedResponderSigningPublicKey)) {
                    "Ready packet refers to a different local signing key"
                }
                check(invitation.remoteEncryptionPublicKey.contentEquals(ready.senderEncryptionPublicKey)) {
                    "Ready sender encryption key does not match the invitation"
                }
                check(invitation.remoteSigningPublicKey.contentEquals(ready.senderSigningPublicKey)) {
                    "Ready sender signing key does not match the invitation"
                }

                if (invitation.stage == IdentityExchangeStage.MUTUAL_UNVERIFIED.name) {
                    return@withLock
                }

                check(
                    invitation.stage == IdentityExchangeStage.ACCEPTANCE_SENT.name ||
                        invitation.stage == IdentityExchangeStage.WAITING_FOR_READY.name
                ) {
                    "Ready confirmation cannot be applied from state ${invitation.stage}"
                }

                if (canImportIdentityFromInvitation()) {
                    remoteIdentityDataSource.markMutual(
                        peerId = invitation.contactId,
                        encryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        signingPublicKey = invitation.remoteSigningPublicKey
                    )
                }

                store.upsert(
                    invitation.copy(
                        stage = IdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )
            }
        }

    /** Called only after orchestration verifies the signed protocol packet. */
    suspend fun recordRemoteDecline(
        exchangeId: String,
        peerId: String,
        inviteChallenge: ByteArray,
        remoteSigningPublicKey: ByteArray,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            require(exchangeId.isNotBlank()) { "Exchange ID must not be blank" }
            require(peerId.isNotBlank()) { "Peer ID must not be blank" }
            mutex.withLock {
                val invitation = store.findById(exchangeId)
                if (invitation == null) {
                    // Terminal invitation responses are replay-safe. Once the exact invitation
                    // is gone, do not compare the packet with the contact's current identity:
                    // the contact may have legitimately re-keyed since this old invitation.
                    // Orchestration has already verified the packet signature and timestamp;
                    // no local state is mutated for this stale response.
                    logger.debug {
                        "Ignoring stale decline for missing invitation $exchangeId"
                    }
                    return@withLock
                }

                check(invitation.direction == IdentityExchangeDirection.OUTGOING.name) {
                    "Invitation direction does not match this operation"
                }
                check(invitation.contactId == peerId) {
                    "Decline contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(inviteChallenge)) {
                    "Decline challenge does not match invitation"
                }
                check(
                    invitation.remoteSigningPublicKey.isEmpty() ||
                        invitation.remoteSigningPublicKey.contentEquals(
                            remoteSigningPublicKey
                        )
                ) {
                    "Contact signing identity changed during invitation decline"
                }

                if (invitation.stage == IdentityExchangeStage.CLOSED.name) {
                    check(invitation.remoteSigningPublicKey.contentEquals(remoteSigningPublicKey)) {
                        "Decline replay changed its signing key"
                    }
                    return@withLock
                }

                requireState(invitation, IdentityExchangeStage.OUTGOING_CHALLENGE_SENT)
                store.upsert(
                    invitation.copy(
                        stage = IdentityExchangeStage.CLOSED.name,
                        remoteSigningPublicKey = remoteSigningPublicKey.copyOf(),
                        updatedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                        lastError = REMOTE_DECLINED_RESULT_MARKER
                    )
                )
            }
        }

    suspend fun getExchangeBinding(exchangeId: String): Result<IdentityExchangeBindingDto?> =
        safeSuspendCall {
            mutex.withLock {
                store.findById(exchangeId)?.let { exchange ->
                    IdentityExchangeBindingDto(
                        exchangeId = exchange.exchangeId,
                        peerId = exchange.contactId,
                        inviteChallenge = exchange.inviteChallenge,
                        remoteSigningPublicKey = exchange.remoteSigningPublicKey
                    )
                }
            }
        }

    /** Persist an invalidation only after the caller has verified the protocol packet. */
    suspend fun invalidateExchange(
        exchangeId: String,
        peerId: String,
        expectedChallenge: ByteArray,
        expectedSigningPublicKey: ByteArray,
        atEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                val exchange = store.findById(exchangeId)
                    ?: error("Invitation was not found: $exchangeId")
                check(exchange.contactId == peerId) {
                    "Authorization revocation contact does not match invitation"
                }
                check(exchange.inviteChallenge.contentEquals(expectedChallenge)) {
                    "Authorization revocation challenge does not match invitation"
                }
                check(exchange.remoteSigningPublicKey.contentEquals(expectedSigningPublicKey)) {
                    "Authorization revocation signing key does not match the contact identity"
                }
                if (exchange.stage != IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue) {
                    store.upsert(
                        exchange.copy(
                            stage = IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue,
                            updatedAtEpochMilliseconds = atEpochMilliseconds,
                            lastError = null
                        )
                    )
                }
            }
        }

    private suspend fun prepareAcceptedRemoteIdentity(invitation: IdentityExchangeEntity) {
        remoteIdentityDataSource.accept(
            peerId = invitation.contactId,
            encryptionPublicKey = invitation.remoteEncryptionPublicKey,
            signingPublicKey = invitation.remoteSigningPublicKey
        )
    }

    private suspend fun stageIncomingInvitationIdentity(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ) {
        val existingIdentity = remoteIdentityDataSource.findByPeerId(contactId)
        val sameIdentity =
            existingIdentity != null &&
                existingIdentity.encryptionPublicKey.contentEquals(remoteEncryptionPublicKey) &&
                existingIdentity.signingPublicKey.contentEquals(remoteSigningPublicKey)
        val pinnedIdentityChanged =
            existingIdentity != null &&
                !sameIdentity &&
                (
                    existingIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name ||
                        existingIdentity.verificationStatus == ContactVerificationStatus.VERIFIED.name
                )

        if (pinnedIdentityChanged) {
            return
        }

        remoteIdentityDataSource.stage(
            peerId = contactId,
            encryptionPublicKey = remoteEncryptionPublicKey,
            signingPublicKey = remoteSigningPublicKey
        )
    }

    private suspend fun hasEstablishedIdentityExchange(
        contactId: String,
        localIdentity: LocalPublicIdentity
    ): Boolean {
        val latestInvitation = store.findLatestForContact(contactId)
        val latestAuthorizationEvent =
            store.findLatestForContactByStages(
                contactId = contactId,
                stages = EXCHANGE_EVENT_STAGES
            )

        val state =
            resolveObservedState(
                latestInvitation = latestInvitation,
                latestAuthorizationEvent = latestAuthorizationEvent
            )
        val authorizationEvent =
            when (state) {
                IdentityHandshakeState.WAITING_FOR_READY -> latestInvitation
                IdentityHandshakeState.MUTUAL_UNVERIFIED -> latestAuthorizationEvent
                else -> null
            }

        return state in ESTABLISHED_EXCHANGE_STATES &&
            authorizationEvent != null &&
            isBoundToLocalIdentity(authorizationEvent, localIdentity)
    }

    private fun isBoundToLocalIdentity(
        invitation: IdentityExchangeEntity,
        localIdentity: LocalPublicIdentity
    ): Boolean =
        invitation.localEncryptionPublicKey?.contentEquals(localIdentity.encryptionPublicKey) == true &&
            invitation.localSigningPublicKey?.contentEquals(localIdentity.signingPublicKey) == true

    private fun resolveObservedState(
        latestInvitation: IdentityExchangeEntity?,
        latestAuthorizationEvent: IdentityExchangeEntity?
    ): IdentityHandshakeState? {
        val authorizationIsCurrent =
            latestAuthorizationEvent?.stage == IdentityExchangeStage.MUTUAL_UNVERIFIED.name &&
                (
                    latestInvitation == null ||
                        latestAuthorizationEvent.updatedAtEpochMilliseconds >=
                        latestInvitation.updatedAtEpochMilliseconds
                )

        if (authorizationIsCurrent) {
            return IdentityHandshakeState.MUTUAL_UNVERIFIED
        }

        return latestInvitation?.stage.toIdentityHandshakeStateOrNull()
    }

    private suspend fun resumeActiveHandshake(invitation: IdentityExchangeEntity): Boolean {
        if (
            invitation.direction == IdentityExchangeDirection.INCOMING.name &&
            (
                invitation.stage == IdentityExchangeStage.ACCEPTANCE_SENT.name ||
                    invitation.stage == IdentityExchangeStage.WAITING_FOR_READY.name
            )
        ) {
            queueAcceptanceReplay(invitation)
            return true
        }

        if (
            invitation.direction == IdentityExchangeDirection.OUTGOING.name &&
            invitation.stage == IdentityExchangeStage.OUTGOING_CHALLENGE_SENT.name
        ) {
            val packetId = invitePacketId(invitation.exchangeId)
            val outboxItem = protocolOutbox.findByPacketId(packetId).getOrThrow()
            return when (outboxItem?.status) {
                OutboxStatus.PENDING,
                OutboxStatus.PROCESSING -> true

                OutboxStatus.SENT ->
                    SystemClock.nowEpochMilliseconds() - invitation.updatedAtEpochMilliseconds <
                        INVITATION_RESTART_GRACE_MILLISECONDS

                OutboxStatus.FAILED,
                OutboxStatus.EXPIRED,
                null -> false
            }
        }

        return true
    }

    private suspend fun recoverIncomingInviteReplay(invitation: IdentityExchangeEntity) {
        when (invitation.stage) {
            IdentityExchangeStage.ACCEPTANCE_SENT.name,
            IdentityExchangeStage.WAITING_FOR_READY.name ->
                queueAcceptanceReplay(invitation)

            IdentityExchangeStage.CLOSED.name ->
                queueDecline(
                    contactId = invitation.contactId,
                    invitationId = invitation.exchangeId,
                    inviteChallenge = invitation.inviteChallenge
                )
        }
    }

    private suspend fun queueAcceptanceReplay(invitation: IdentityExchangeEntity) {
        if (canImportIdentityFromInvitation()) prepareAcceptedRemoteIdentity(invitation)
        val responseChallenge =
            checkNotNull(invitation.responseChallenge) {
                "Accepted invitation is missing its response challenge"
            }
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireLocalKeysMatch(localIdentity, signingKeyPair)
        val acceptedAt = SystemClock.nowEpochMilliseconds()
        val profilePicture = localProfilePictureMetadataProvider.forInvite().getOrElse { ProfilePictureMetadata() }
        check(acceptedAt <= invitation.expiresAtEpochMilliseconds) {
            "Invitation has expired"
        }
        val packet = invitationHandshakeProtocol.createAccepted(
            invitationId = invitation.exchangeId,
            acceptedAtEpochMilliseconds = acceptedAt,
            profilePicture = profilePicture,
            inviteChallenge = invitation.inviteChallenge,
            responseChallenge = responseChallenge,
            inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
            inviterSigningPublicKey = invitation.remoteSigningPublicKey,
            responderEncryptionPublicKey = localIdentity.encryptionPublicKey,
            signingKeyPair = signingKeyPair
        ).getOrThrow()
        enqueueOrResend(contactId = invitation.contactId, packet = packet).getOrThrow()
        if (canImportIdentityFromInvitation()) {
            remoteIdentityDataSource.markMutual(
                peerId = invitation.contactId,
                encryptionPublicKey = invitation.remoteEncryptionPublicKey,
                signingPublicKey = invitation.remoteSigningPublicKey
            )
        }
        store.upsert(
            invitation.copy(
                stage = IdentityExchangeStage.WAITING_FOR_READY.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                lastError = null,
                localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
            )
        )
    }

    private suspend fun queueReadyReplay(
        contactId: String,
        acceptance: IdentityExchangeAcceptance
    ) {
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireLocalKeysMatch(localIdentity, signingKeyPair)
        val readyAt = SystemClock.nowEpochMilliseconds()
        val readyPacket = invitationHandshakeProtocol.createReady(
            invitationId = acceptance.exchangeId,
            readyAtEpochMilliseconds = readyAt,
            responseChallenge = acceptance.responseChallenge,
            acceptedResponderEncryptionPublicKey = acceptance.responderEncryptionPublicKey,
            acceptedResponderSigningPublicKey = acceptance.responderSigningPublicKey,
            senderEncryptionPublicKey = localIdentity.encryptionPublicKey,
            signingKeyPair = signingKeyPair
        ).getOrThrow()
        enqueueOrResend(contactId = contactId, packet = readyPacket).getOrThrow()
    }

    private suspend fun enqueueOrResend(
        contactId: String,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val existing = protocolOutbox.findByPacketId(packet.packetId).getOrThrow()
            if (existing == null) {
                protocolOutbox.enqueue(contactId, packet).getOrThrow()
            } else {
                protocolOutbox.resend(packet.packetId).getOrThrow()
            }
        }

    private suspend fun resendPersistedPacket(packetId: String): Boolean {
        val existing = protocolOutbox.findByPacketId(packetId).getOrThrow() ?: return false
        protocolOutbox.resend(existing.packetId).getOrThrow()
        return true
    }

    private fun String?.toIdentityHandshakeStateOrNull(): IdentityHandshakeState? =
        when (this) {
            IdentityExchangeStage.ACCEPTANCE_SENT.name -> IdentityHandshakeState.ACCEPTANCE_SENT
            IdentityExchangeStage.WAITING_FOR_READY.name -> IdentityHandshakeState.WAITING_FOR_READY
            IdentityExchangeStage.MUTUAL_UNVERIFIED.name -> IdentityHandshakeState.MUTUAL_UNVERIFIED
            IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue -> IdentityHandshakeState.EXCHANGE_INVALIDATED
            IdentityExchangeStage.FAILED.name -> IdentityHandshakeState.FAILED
            else -> null
        }

    private fun invitePacketId(invitationId: String): String = "contact-invite-$invitationId"

    private fun declinedPacketId(invitationId: String): String = "contact-invite-declined-$invitationId"

    private suspend fun requireInvitation(
        invitationId: String,
        direction: IdentityExchangeDirection
    ): IdentityExchangeEntity {
        require(invitationId.isNotBlank()) {
            "Invitation ID must not be blank"
        }

        val invitation = store.findById(invitationId) ?: error("Invitation was not found: $invitationId")
        check(invitation.direction == direction.name) {
            "Invitation direction does not match this operation"
        }
        return invitation
    }

    private suspend fun ensureNotExpired(invitation: IdentityExchangeEntity) {
        if (invitation.expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
            return
        }

        store.upsert(
            invitation.copy(
                stage = IdentityExchangeStage.CLOSED.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                lastError = "Invitation expired"
            )
        )
        error("Invitation has expired")
    }

    private fun requireState(
        invitation: IdentityExchangeEntity,
        expectedState: IdentityExchangeStage
    ) {
        check(invitation.stage == expectedState.name) {
            "Expected invitation state ${expectedState.name}, but was ${invitation.stage}"
        }
    }

    private fun requireLocalKeysMatch(
        identity: LocalPublicIdentity,
        signingKeyPair: LocalSigningKeyPair
    ) {
        check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local signing key pair does not match the public identity"
        }
    }

    private suspend fun queueDecline(
        contactId: String,
        invitationId: String,
        inviteChallenge: ByteArray
    ) {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val packet = invitationDeclineProtocol.createPacket(
            exchangeId = invitationId,
            inviteChallenge = inviteChallenge,
            declinedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
            signingKeyPair = signingKeyPair
        ).getOrThrow()
        enqueueOrResend(contactId = contactId, packet = packet).getOrThrow()
    }

    private fun toResult(exchange: IdentityExchangeEntity): IdentityResult? {
        val direction =
            IdentityExchangeDirection.entries.firstOrNull { candidate -> candidate.name == exchange.direction }
                ?: return null
        val type =
            when {
                exchange.stage == IdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED.name &&
                    direction == IdentityExchangeDirection.INCOMING ->
                    IdentityResultStatus.INCOMING_EXCHANGE

                exchange.stage == IdentityExchangeStage.MUTUAL_UNVERIFIED.name ->
                    IdentityResultStatus.ESTABLISHED

                exchange.stage == IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue ->
                    IdentityResultStatus.EXCHANGE_INVALIDATED

                exchange.stage == IdentityExchangeStage.FAILED.name ->
                    IdentityResultStatus.FAILED

                exchange.stage == IdentityExchangeStage.CLOSED.name &&
                    direction == IdentityExchangeDirection.OUTGOING &&
                    exchange.lastError == REMOTE_DECLINED_RESULT_MARKER ->
                    IdentityResultStatus.REMOTE_DECLINED

                else -> return null
            }

        return IdentityResult(
            exchangeId = exchange.exchangeId,
            peerId = exchange.contactId,
            direction = direction,
            status = type,
            createdAtEpochMilliseconds = exchange.createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = exchange.expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = maxOf(exchange.createdAtEpochMilliseconds, exchange.updatedAtEpochMilliseconds),
            wasKnownPeerAtReceive = exchange.wasKnownPeerAtReceive
        )
    }

    private fun IdentityExchangeEntity.toLifecycleRecord(): IdentityExchange =
        IdentityExchange(
            exchangeId = exchangeId,
            peerId = contactId,
            direction =
                IdentityExchangeDirection.entries.firstOrNull { direction -> direction.name == this.direction }
                    ?: error("Unknown invitation direction: $direction"),
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = maxOf(createdAtEpochMilliseconds, updatedAtEpochMilliseconds)
        )

    private companion object {
        const val CHALLENGE_SIZE = 32
        const val INVITATION_LIFETIME_MILLISECONDS = 24L * 60L * 60L * 1_000L
        const val INVITATION_RESTART_GRACE_MILLISECONDS = 5L * 1_000L
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
        const val REMOTE_DECLINED_RESULT_MARKER = "REMOTE_DECLINED"

        val ESTABLISHED_EXCHANGE_STATES =
            setOf(
                IdentityHandshakeState.WAITING_FOR_READY,
                IdentityHandshakeState.MUTUAL_UNVERIFIED
            )

        val EXCHANGE_EVENT_STAGES =
            listOf(
                IdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue
            )

        val TERMINAL_STATES =
            listOf(
                IdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                IdentityExchangeStage.CLOSED.name,
                IdentityExchangeStage.EXCHANGE_INVALIDATED.persistedValue,
                IdentityExchangeStage.FAILED.name
            )
    }
}
