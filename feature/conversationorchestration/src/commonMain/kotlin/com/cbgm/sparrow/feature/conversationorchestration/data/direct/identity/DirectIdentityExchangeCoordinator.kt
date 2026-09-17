package com.cbgm.sparrow.feature.conversationorchestration.data.direct.identity

import com.cbgm.sparrow.core.crypto.random.SecureRandomGenerator
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.OutboxStatus
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.core.protocol.packet.DirectChatAuthorizationRevokedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.ContactRoutingIdDao
import com.cbgm.sparrow.data.database.dao.IdentityExchangeDao
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactRoutingIdEntity
import com.cbgm.sparrow.data.database.entity.IdentityExchangeEntity
import com.cbgm.sparrow.feature.conversationorchestration.data.direct.authorization.DirectAuthorizationPayloadEncoder
import com.cbgm.sparrow.feature.identity.data.datasource.ContactKeyExchangeDataSource
import com.cbgm.sparrow.feature.identity.data.datasource.ContactVerificationDataSource
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.identity.domain.model.RemoteIdentityOrigin
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPayloadEncoder
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
internal class DirectIdentityExchangeCoordinator(
    private val identityExchangeDao: IdentityExchangeDao,
    private val contactDao: ContactDao,
    private val contactRoutingIdDao: ContactRoutingIdDao,
    private val contactKeyExchangeDataSource: ContactKeyExchangeDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val secureRandomGenerator: SecureRandomGenerator,
    private val payloadEncoder: InvitationPayloadEncoder,
    private val authorizationPayloadEncoder: DirectAuthorizationPayloadEncoder,
    private val protocolOutbox: ProtocolOutbox,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val contactVerificationDataSource: ContactVerificationDataSource,
    private val localProfilePictureMetadataProvider: LocalProfilePictureMetadataProvider,
    private val remoteProfilePictureMetadataProcessor: RemoteProfilePictureMetadataProcessor
) {
    private val logger = SparrowLog.withTag("DirectIdentityExchangeCoordinator")

    private val mutex = Mutex()

    suspend fun start(contactId: String): Result<Unit> =
        startInvitation(contactId).map { Unit }

    suspend fun startInvitation(contactId: String): Result<InvitationLifecycleRecord?> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            mutex.withLock {
                val contact = contactDao.findById(contactId) ?: error("Contact was not found: $contactId")
                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)

                if (hasActiveDirectChatAuthorization(contactId, localIdentity)) {
                    return@withLock null
                }

                val now = SystemClock.nowEpochMilliseconds()
                identityExchangeDao.findActiveForContact(contactId, TERMINAL_STATES)?.let { activeInvitation ->
                    if (!isBoundToLocalIdentity(activeInvitation, localIdentity)) {
                        identityExchangeDao.upsert(
                            activeInvitation.copy(
                                stage = DirectIdentityExchangeStage.FAILED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Handshake belongs to a previous local identity"
                            )
                        )
                    } else if (activeInvitation.expiresAtEpochMilliseconds > now) {
                        if (resumeActiveHandshake(activeInvitation)) {
                            return@withLock activeInvitation
                                .takeIf { invitation ->
                                    invitation.direction == InvitationDirection.OUTGOING.name &&
                                        invitation.stage == DirectIdentityExchangeStage.OUTGOING_CHALLENGE_SENT.name
                                }?.toLifecycleRecord()
                        }

                        identityExchangeDao.upsert(
                            activeInvitation.copy(
                                stage = DirectIdentityExchangeStage.FAILED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Handshake was superseded by a fresh invitation"
                            )
                        )
                    } else {
                        identityExchangeDao.upsert(
                            activeInvitation.copy(
                                stage = DirectIdentityExchangeStage.CLOSED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Invitation expired"
                            )
                        )
                    }
                }

                val invitationId = IdGenerator.generate()
                val packetId = invitePacketId(invitationId)
                val challenge = secureRandomGenerator.generateBytes(CHALLENGE_SIZE).getOrThrow()
                val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
                val expiresAt = now + INVITATION_LIFETIME_MILLISECONDS
                val profilePicture = localProfilePictureMetadataProvider.forInvite().getOrElse { ProfilePictureMetadata() }
                val payload =
                    payloadEncoder.encodeInvite(
                        packetId = packetId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = invitationId,
                        displayName = localPhoneNumber,
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        profilePicture = profilePicture,
                        inviteChallenge = challenge,
                        encryptionPublicKey = localIdentity.encryptionPublicKey,
                        signingPublicKey = localIdentity.signingPublicKey
                    )
                val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
                val packet =
                    ContactInvitePacket(
                        packetId = packetId,
                        invitationId = invitationId,
                        displayName = localPhoneNumber,
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        profilePicture = profilePicture,
                        inviteChallenge = challenge.copyOf(),
                        encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = localIdentity.signingPublicKey.copyOf(),
                        signature = signature.copyOf()
                    )

                val storedInvitation =
                    IdentityExchangeEntity(
                        exchangeId = invitationId,
                        contactId = contactId,
                        direction = InvitationDirection.OUTGOING.name,
                        stage = DirectIdentityExchangeStage.OUTGOING_CHALLENGE_SENT.name,
                        remoteDisplayName = contact.contact.displayName,
                        inviteChallenge = challenge.copyOf(),
                        responseChallenge = null,
                        remoteEncryptionPublicKey =
                            contact.publicIdentity?.encryptionPublicKey?.copyOf() ?: byteArrayOf(),
                        remoteSigningPublicKey = contact.publicIdentity?.signingPublicKey?.copyOf() ?: byteArrayOf(),
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        updatedAtEpochMilliseconds = now,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                identityExchangeDao.upsert(storedInvitation)

                enqueueOrResend(contactId, packet).getOrElse { error ->
                    identityExchangeDao.upsert(
                        requireNotNull(identityExchangeDao.findById(invitationId)).copy(
                            stage = DirectIdentityExchangeStage.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }

                storedInvitation.toLifecycleRecord()
            }
        }

    suspend fun getPendingIncomingInvitationLifecycleRecord(
        invitationId: String
    ): Result<InvitationLifecycleRecord?> =
        safeSuspendCall {
            require(invitationId.isNotBlank()) { "Invitation ID must not be blank" }
            val invitation = identityExchangeDao.findById(invitationId) ?: return@safeSuspendCall null
            if (invitation.direction != InvitationDirection.INCOMING.name) return@safeSuspendCall null
            if (invitation.stage != DirectIdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED.name) return@safeSuspendCall null
            invitation.toLifecycleRecord()
        }

    fun observeState(contactId: String): Flow<IdentityHandshakeState?> {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        return combine(
            identityExchangeDao.observeLatestForContact(contactId),
            identityExchangeDao.observeLatestForContactByStages(
                contactId = contactId,
                stages = AUTHORIZATION_EVENT_STATES
            )
        ) { latestInvitation, latestAuthorizationEvent ->
            val state =
                resolveObservedState(
                    latestInvitation = latestInvitation,
                    latestAuthorizationEvent = latestAuthorizationEvent
                )
            if (state !in DIRECT_CHAT_AUTHORIZED_STATES) {
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
                var invitation = requireInvitation(invitationId, InvitationDirection.INCOMING)
                ensureNotExpired(invitation)
                invitation = rebindIncomingInvitation(invitation)

                if (
                    invitation.stage == DirectIdentityExchangeStage.ACCEPTANCE_SENT.name ||
                    invitation.stage == DirectIdentityExchangeStage.WAITING_FOR_READY.name
                ) {
                    queueAcceptanceReplay(invitation)
                    return@withLock
                }
                if (invitation.stage == DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name) {
                    return@withLock
                }

                requireState(invitation, DirectIdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED)

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)

                val now = SystemClock.nowEpochMilliseconds()
                val profilePicture = localProfilePictureMetadataProvider.forInvite().getOrElse { ProfilePictureMetadata() }
                val responseChallenge = secureRandomGenerator.generateBytes(CHALLENGE_SIZE).getOrThrow()
                val packetId = acceptedPacketId(invitationId)
                val payload =
                    payloadEncoder.encodeAccepted(
                        packetId = packetId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = invitationId,
                        acceptedAtEpochMilliseconds = now,
                        profilePicture = profilePicture,
                        inviteChallenge = invitation.inviteChallenge,
                        responseChallenge = responseChallenge,
                        inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        inviterSigningPublicKey = invitation.remoteSigningPublicKey,
                        responderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                        responderSigningPublicKey = localIdentity.signingPublicKey
                    )
                val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
                val packet =
                    ContactInviteAcceptedPacket(
                        packetId = packetId,
                        invitationId = invitationId,
                        acceptedAtEpochMilliseconds = now,
                        profilePicture = profilePicture,
                        inviteChallenge = invitation.inviteChallenge.copyOf(),
                        responseChallenge = responseChallenge.copyOf(),
                        inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey.copyOf(),
                        inviterSigningPublicKey = invitation.remoteSigningPublicKey.copyOf(),
                        responderEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        responderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                        signature = signature.copyOf()
                    )

                prepareAcceptedRemoteIdentity(invitation)
                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.ACCEPTANCE_SENT.name,
                        responseChallenge = responseChallenge.copyOf(),
                        updatedAtEpochMilliseconds = now,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )
                enqueueOrResend(invitation.contactId, packet).getOrElse { error ->
                    identityExchangeDao.upsert(
                        requireNotNull(identityExchangeDao.findById(invitationId)).copy(
                            stage = DirectIdentityExchangeStage.ACCEPTANCE_SENT.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
                contactKeyExchangeDataSource
                    .markMutual(
                        contactId = invitation.contactId,
                        expectedRemoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = invitation.remoteSigningPublicKey
                    )
                identityExchangeDao.upsert(
                    requireNotNull(identityExchangeDao.findById(invitationId)).copy(
                        stage = DirectIdentityExchangeStage.WAITING_FOR_READY.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            }
        }

    suspend fun decline(invitationId: String): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                val invitation = requireInvitation(invitationId, InvitationDirection.INCOMING)
                if (invitation.stage == DirectIdentityExchangeStage.CLOSED.name) {
                    resendPersistedPacket(declinedPacketId(invitation.exchangeId))
                    return@withLock
                }
                requireState(invitation, DirectIdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED)

                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val now = SystemClock.nowEpochMilliseconds()
                val packetId = declinedPacketId(invitationId)
                val payload =
                    payloadEncoder.encodeDeclined(
                        packetId = packetId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = invitationId,
                        declinedAtEpochMilliseconds = now,
                        inviteChallenge = invitation.inviteChallenge,
                        declinerSigningPublicKey = signingKeyPair.publicKey
                    )
                val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
                val packet =
                    ContactInviteDeclinedPacket(
                        packetId = packetId,
                        invitationId = invitationId,
                        declinedAtEpochMilliseconds = now,
                        inviteChallenge = invitation.inviteChallenge.copyOf(),
                        declinerSigningPublicKey = signingKeyPair.publicKey.copyOf(),
                        signature = signature.copyOf()
                    )

                enqueueOrResend(invitation.contactId, packet).getOrElse { error ->
                    identityExchangeDao.upsert(
                        invitation.copy(
                            stage = DirectIdentityExchangeStage.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.CLOSED.name,
                        updatedAtEpochMilliseconds = now,
                        lastError = null
                    )
                )
            }
        }

    suspend fun cancelForManualSetup(contactId: String): Result<Unit> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            mutex.withLock {
                val invitation =
                    identityExchangeDao.findActiveForContact(
                        contactId = contactId,
                        terminalStages = TERMINAL_STATES
                    ) ?: return@withLock

                val state =
                    DirectIdentityExchangeStage.entries.firstOrNull { candidate ->
                        candidate.name == invitation.stage
                    } ?: return@withLock

                when {
                    invitation.direction == InvitationDirection.INCOMING.name &&
                        state == DirectIdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED -> {
                        queueDecline(
                            contactId = invitation.contactId,
                            invitationId = invitation.exchangeId,
                            inviteChallenge = invitation.inviteChallenge
                        )

                        identityExchangeDao.upsert(
                            invitation.copy(
                                stage = DirectIdentityExchangeStage.CLOSED.name,
                                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                                lastError = "Manual identity exchange selected"
                            )
                        )
                    }

                    invitation.direction == InvitationDirection.OUTGOING.name &&
                        state == DirectIdentityExchangeStage.OUTGOING_CHALLENGE_SENT -> {
                        identityExchangeDao.upsert(
                            invitation.copy(
                                stage = DirectIdentityExchangeStage.CLOSED.name,
                                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                                lastError = "Manual identity exchange selected"
                            )
                        )
                    }
                }
            }
        }

    suspend fun requireDirectChatAuthorization(
        contactId: String,
        mode: DirectIdentitySetupMode
    ): Result<Unit> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            when (mode) {
                DirectIdentitySetupMode.AUTOMATIC_INVITATION -> {
                    val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                    if (!hasActiveDirectChatAuthorization(contactId, localIdentity)) {
                        throw DirectChatAuthorizationRequiredException(
                            "A contact invitation must be accepted before messages can be sent"
                        )
                    }
                }

                DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING -> {
                    val keyExchangeStatus =
                        contactDao
                            .findPublicIdentityByContactId(contactId)
                            ?.keyExchangeStatus
                    if (keyExchangeStatus != KeyExchangeStatus.MUTUAL.name) {
                        throw DirectChatAuthorizationRequiredException(
                            "Both identities must be exchanged before messages can be sent"
                        )
                    }
                }
            }
        }

    suspend fun revokeDirectChatAuthorization(contactId: String): Result<Unit> =
        safeSuspendCall {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            mutex.withLock {
                val invitation = identityExchangeDao.findLatestForContact(contactId) ?: return@withLock
                val state =
                    DirectIdentityExchangeStage.entries.firstOrNull { candidate ->
                        candidate.name == invitation.stage
                    } ?: error("Unknown contact invitation state: ${invitation.stage}")

                if (state == DirectIdentityExchangeStage.AUTHORIZATION_REVOKED) {
                    return@withLock
                }

                if (
                    invitation.direction == InvitationDirection.INCOMING.name &&
                    state == DirectIdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED
                ) {
                    queueDecline(
                        contactId = contactId,
                        invitationId = invitation.exchangeId,
                        inviteChallenge = invitation.inviteChallenge
                    )
                } else if (
                    state != DirectIdentityExchangeStage.CLOSED &&
                    state != DirectIdentityExchangeStage.FAILED
                ) {
                    queueDirectChatAuthorizationRevocation(invitation)
                }

                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.AUTHORIZATION_REVOKED.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                        lastError = null
                    )
                )
            }
        }

    suspend fun receiveInvite(
        context: IncomingPacketContext,
        packet: ContactInvitePacket,
        receptionEnabled: Boolean,
        blockedPeerIds: Set<String>,
        blockUnknownPeers: Boolean
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-invite",
                    invitationId = packet.invitationId
                )
                val payload =
                    payloadEncoder.encodeInvite(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        displayName = packet.displayName,
                        createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                        profilePicture = packet.profilePicture,
                        inviteChallenge = packet.inviteChallenge,
                        encryptionPublicKey = packet.encryptionPublicKey,
                        signingPublicKey = packet.signingPublicKey
                    )
                detachedSignatureCrypto.verify(payload, packet.signingPublicKey, packet.signature).getOrThrow()
                require(
                    packet.createdAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Invitation was created too far in the future"
                }
                require(packet.expiresAtEpochMilliseconds > packet.createdAtEpochMilliseconds) {
                    "Invitation expiry must be after its creation time"
                }
                require(
                    packet.expiresAtEpochMilliseconds - packet.createdAtEpochMilliseconds <=
                        INVITATION_LIFETIME_MILLISECONDS
                ) {
                    "Invitation lifetime exceeds the allowed maximum"
                }
                require(packet.expiresAtEpochMilliseconds > context.receivedAtEpochMilliseconds) {
                    "Invitation has expired"
                }

                val remoteDisplayName =
                    packet.displayName
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                val remotePhoneNumber =
                    remoteDisplayName
                        ?.let { value -> phoneNumberNormalizer.normalize(value).getOrNull() }
                val contactId =
                    resolveIncomingInviteContactId(
                        resolvedContactId = context.contactId,
                        remotePhoneNumber = remotePhoneNumber,
                        remoteEncryptionPublicKey = packet.encryptionPublicKey,
                        remoteSigningPublicKey = packet.signingPublicKey
                    )
                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

                if (
                    shouldAutomaticallyDecline(
                        contactId = contactId,
                        blockedPeerIds = blockedPeerIds,
                        blockUnknownPeers = blockUnknownPeers
                    )
                ) {
                    queueDecline(
                        contactId = contactId,
                        invitationId = packet.invitationId,
                        inviteChallenge = packet.inviteChallenge
                    )
                    return@withLock
                }

                if (!receptionEnabled) {
                    queueDecline(
                        contactId = contactId,
                        invitationId = packet.invitationId,
                        inviteChallenge = packet.inviteChallenge
                    )
                    return@withLock
                }

                remoteProfilePictureMetadataProcessor
                    .apply(contactId, packet.profilePicture)
                    .onFailure { error ->
                        logger.warn(error) { "Could not store profile picture for $contactId" }
                    }

                remotePhoneNumber?.let { phoneNumber ->
                    persistIncomingPhoneNumber(
                        contactId = contactId,
                        phoneNumber = phoneNumber,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                    )
                }

                identityExchangeDao.findById(packet.invitationId)?.let { existing ->
                    check(existing.direction == InvitationDirection.INCOMING.name) {
                        "Invitation replay changed its direction"
                    }
                    check(existing.contactId == contactId) {
                        "Invitation replay used a different contact"
                    }
                    check(existing.remoteDisplayName == remoteDisplayName) {
                        "Invitation replay changed its display name"
                    }
                    check(existing.createdAtEpochMilliseconds == packet.createdAtEpochMilliseconds) {
                        "Invitation replay changed its creation time"
                    }
                    check(existing.expiresAtEpochMilliseconds == packet.expiresAtEpochMilliseconds) {
                        "Invitation replay changed its expiry time"
                    }
                    check(existing.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                        "Invitation replay changed its challenge"
                    }
                    check(existing.remoteEncryptionPublicKey.contentEquals(packet.encryptionPublicKey)) {
                        "Invitation replay changed its encryption key"
                    }
                    check(existing.remoteSigningPublicKey.contentEquals(packet.signingPublicKey)) {
                        "Invitation replay changed its signing key"
                    }
                    val reboundExisting =
                        existing.copy(
                            localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                            localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                        )
                    identityExchangeDao.upsert(reboundExisting)
                    recoverIncomingInviteReplay(reboundExisting)
                    return@withLock
                }

                identityExchangeDao
                    .findActiveForContact(
                        contactId = contactId,
                        terminalStages = TERMINAL_STATES
                    )?.let { activeInvitation ->
                        if (activeInvitation.expiresAtEpochMilliseconds <= context.receivedAtEpochMilliseconds) {
                            identityExchangeDao.upsert(
                                activeInvitation.copy(
                                    stage = DirectIdentityExchangeStage.CLOSED.name,
                                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                                    lastError = "Invitation expired"
                                )
                            )
                        } else {
                            check(
                                activeInvitation.remoteEncryptionPublicKey.isEmpty() ||
                                    activeInvitation.remoteEncryptionPublicKey.contentEquals(
                                        packet.encryptionPublicKey
                                    )
                            ) {
                                "Another active contact invitation pins a different encryption key"
                            }
                            check(
                                activeInvitation.remoteSigningPublicKey.isEmpty() ||
                                    activeInvitation.remoteSigningPublicKey.contentEquals(
                                        packet.signingPublicKey
                                    )
                            ) {
                                "Another active contact invitation pins a different signing key"
                            }
                        }
                    }

                remotePhoneNumber?.let { phoneNumber ->
                    contactDao.usePhoneNumberAsDisplayNameWhenMissing(
                        contactId = contactId,
                        phoneNumber = phoneNumber,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                    )
                }

                stageIncomingInvitationIdentity(
                    contactId = contactId,
                    remoteEncryptionPublicKey = packet.encryptionPublicKey,
                    remoteSigningPublicKey = packet.signingPublicKey
                )

                identityExchangeDao.upsert(
                    IdentityExchangeEntity(
                        exchangeId = packet.invitationId,
                        contactId = contactId,
                        direction = InvitationDirection.INCOMING.name,
                        stage = DirectIdentityExchangeStage.INCOMING_CHALLENGE_RECEIVED.name,
                        remoteDisplayName = remoteDisplayName,
                        inviteChallenge = packet.inviteChallenge.copyOf(),
                        responseChallenge = null,
                        remoteEncryptionPublicKey = packet.encryptionPublicKey.copyOf(),
                        remoteSigningPublicKey = packet.signingPublicKey.copyOf(),
                        createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )
            }
        }

    suspend fun receiveAccepted(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-invite-accepted",
                    invitationId = packet.invitationId
                )
                val invitation = requireInvitation(packet.invitationId, InvitationDirection.OUTGOING)
                check(invitation.contactId == context.contactId) {
                    "Acceptance contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                    "Acceptance challenge does not match invitation"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(localIdentity.encryptionPublicKey.contentEquals(packet.inviterEncryptionPublicKey)) {
                    "Acceptance refers to a different local encryption key"
                }
                check(localIdentity.signingPublicKey.contentEquals(packet.inviterSigningPublicKey)) {
                    "Acceptance refers to a different local signing key"
                }

                val payload =
                    payloadEncoder.encodeAccepted(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        acceptedAtEpochMilliseconds = packet.acceptedAtEpochMilliseconds,
                        profilePicture = packet.profilePicture,
                        inviteChallenge = packet.inviteChallenge,
                        responseChallenge = packet.responseChallenge,
                        inviterEncryptionPublicKey = packet.inviterEncryptionPublicKey,
                        inviterSigningPublicKey = packet.inviterSigningPublicKey,
                        responderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        responderSigningPublicKey = packet.responderSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, packet.responderSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.acceptedAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Acceptance was created too far in the future"
                }
                require(packet.acceptedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                    "Acceptance was created after the invitation expired"
                }
                check(
                    invitation.remoteEncryptionPublicKey.isEmpty() ||
                        invitation.remoteEncryptionPublicKey.contentEquals(
                            packet.responderEncryptionPublicKey
                        )
                ) {
                    "Contact encryption identity changed during invitation acceptance"
                }
                check(
                    invitation.remoteSigningPublicKey.isEmpty() ||
                        invitation.remoteSigningPublicKey.contentEquals(
                            packet.responderSigningPublicKey
                        )
                ) {
                    "Contact signing identity changed during invitation acceptance"
                }

                remoteProfilePictureMetadataProcessor
                    .apply(context.contactId, packet.profilePicture)
                    .onFailure { error ->
                        logger.warn(error) { "Could not store profile picture for ${context.contactId}" }
                    }

                if (invitation.stage == DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name) {
                    check(invitation.responseChallenge?.contentEquals(packet.responseChallenge) == true) {
                        "Acceptance replay changed its response challenge"
                    }
                    check(invitation.remoteEncryptionPublicKey.contentEquals(packet.responderEncryptionPublicKey)) {
                        "Acceptance replay changed its encryption key"
                    }
                    check(invitation.remoteSigningPublicKey.contentEquals(packet.responderSigningPublicKey)) {
                        "Acceptance replay changed its signing key"
                    }
                    queueReadyReplay(
                        contactId = context.contactId,
                        packet = packet
                    )
                    return@withLock
                }

                requireState(invitation, DirectIdentityExchangeStage.OUTGOING_CHALLENGE_SENT)

                contactKeyExchangeDataSource
                    .storeRemoteIdentity(
                        contactId = context.contactId,
                        encryptionPublicKey = packet.responderEncryptionPublicKey,
                        signingPublicKey = packet.responderSigningPublicKey,
                        origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
                    )
                contactKeyExchangeDataSource
                    .acceptRemoteIdentityForHandshake(
                        contactId = context.contactId,
                        expectedRemoteEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = packet.responderSigningPublicKey
                    )
                val now = SystemClock.nowEpochMilliseconds()
                queueReadyReplay(
                    contactId = context.contactId,
                    packet = packet
                )
                contactKeyExchangeDataSource
                    .markMutual(
                        contactId = context.contactId,
                        expectedRemoteEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = packet.responderSigningPublicKey
                    )

                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                        responseChallenge = packet.responseChallenge.copyOf(),
                        remoteEncryptionPublicKey = packet.responderEncryptionPublicKey.copyOf(),
                        remoteSigningPublicKey = packet.responderSigningPublicKey.copyOf(),
                        updatedAtEpochMilliseconds = now,
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )

                try {
                    contactVerificationDataSource.sendReceiptIfLocallyVerified(context.contactId)
                } catch (error: Throwable) {
                    logger.warn(error) { "Could not queue contact verification receipt" }
                }
            }
        }

    suspend fun receiveReady(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-ready",
                    invitationId = packet.invitationId
                )
                check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                    "ContactReadyPacket must be received through encrypted transport"
                }

                var invitation = requireInvitation(packet.invitationId, InvitationDirection.INCOMING)
                val originalContactId = invitation.contactId
                invitation = rebindIncomingInvitation(invitation)
                check(
                    invitation.contactId == context.contactId ||
                        originalContactId == context.contactId
                ) {
                    "Ready contact does not match invitation"
                }
                check(invitation.responseChallenge?.contentEquals(packet.responseChallenge) == true) {
                    "Ready challenge does not match acceptance"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(localIdentity.encryptionPublicKey.contentEquals(packet.acceptedResponderEncryptionPublicKey)) {
                    "Ready packet refers to a different local encryption key"
                }
                check(localIdentity.signingPublicKey.contentEquals(packet.acceptedResponderSigningPublicKey)) {
                    "Ready packet refers to a different local signing key"
                }
                check(invitation.remoteEncryptionPublicKey.contentEquals(packet.senderEncryptionPublicKey)) {
                    "Ready sender encryption key does not match the invitation"
                }
                check(invitation.remoteSigningPublicKey.contentEquals(packet.senderSigningPublicKey)) {
                    "Ready sender signing key does not match the invitation"
                }

                val payload =
                    payloadEncoder.encodeReady(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        readyAtEpochMilliseconds = packet.readyAtEpochMilliseconds,
                        responseChallenge = packet.responseChallenge,
                        acceptedResponderEncryptionPublicKey = packet.acceptedResponderEncryptionPublicKey,
                        acceptedResponderSigningPublicKey = packet.acceptedResponderSigningPublicKey,
                        senderEncryptionPublicKey = packet.senderEncryptionPublicKey,
                        senderSigningPublicKey = packet.senderSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, invitation.remoteSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.readyAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Ready confirmation was created too far in the future"
                }

                if (invitation.stage == DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name) {
                    return@withLock
                }

                check(
                    invitation.stage == DirectIdentityExchangeStage.ACCEPTANCE_SENT.name ||
                        invitation.stage == DirectIdentityExchangeStage.WAITING_FOR_READY.name
                ) {
                    "Ready confirmation cannot be applied from state ${invitation.stage}"
                }

                contactKeyExchangeDataSource
                    .markMutual(
                        contactId = invitation.contactId,
                        expectedRemoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = invitation.remoteSigningPublicKey
                    )

                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                        lastError = null,
                        localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
                    )
                )

                try {
                    contactVerificationDataSource.sendReceiptIfLocallyVerified(invitation.contactId)
                } catch (error: Throwable) {
                    logger.warn(error) { "Could not queue contact verification receipt" }
                }
            }
        }

    suspend fun receiveDeclined(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                require(packet.invitationId.isNotBlank()) {
                    "Invitation ID must not be blank"
                }
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-invite-declined",
                    invitationId = packet.invitationId
                )

                val payload =
                    payloadEncoder.encodeDeclined(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        declinedAtEpochMilliseconds = packet.declinedAtEpochMilliseconds,
                        inviteChallenge = packet.inviteChallenge,
                        declinerSigningPublicKey = packet.declinerSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, packet.declinerSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.declinedAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Decline response was created too far in the future"
                }

                val invitation = identityExchangeDao.findById(packet.invitationId)
                if (invitation == null) {
                    // Terminal invitation responses are replay-safe. Once the exact invitation
                    // is gone, do not compare the packet with the contact's current identity:
                    // the contact may have legitimately re-keyed since this old invitation.
                    // The packet has already passed its own signature and timestamp checks and
                    // no local state is mutated for this stale response.
                    logger.debug {
                        "Ignoring stale decline for missing invitation ${packet.invitationId}"
                    }
                    return@withLock
                }

                check(invitation.direction == InvitationDirection.OUTGOING.name) {
                    "Invitation direction does not match this operation"
                }
                check(invitation.contactId == context.contactId) {
                    "Decline contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                    "Decline challenge does not match invitation"
                }
                check(
                    invitation.remoteSigningPublicKey.isEmpty() ||
                        invitation.remoteSigningPublicKey.contentEquals(
                            packet.declinerSigningPublicKey
                        )
                ) {
                    "Contact signing identity changed during invitation decline"
                }

                if (invitation.stage == DirectIdentityExchangeStage.CLOSED.name) {
                    check(invitation.remoteSigningPublicKey.contentEquals(packet.declinerSigningPublicKey)) {
                        "Decline replay changed its signing key"
                    }
                    return@withLock
                }

                requireState(invitation, DirectIdentityExchangeStage.OUTGOING_CHALLENGE_SENT)
                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.CLOSED.name,
                        remoteSigningPublicKey = packet.declinerSigningPublicKey.copyOf(),
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        lastError = null
                    )
                )
            }
        }

    suspend fun receiveDirectChatAuthorizationRevoked(
        context: IncomingPacketContext,
        packet: DirectChatAuthorizationRevokedPacket
    ): Result<Unit> =
        safeSuspendCall {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "direct-chat-authorization-revoked",
                    invitationId = packet.invitationId
                )
                val invitation =
                    identityExchangeDao.findById(packet.invitationId)
                        ?: error("Invitation was not found: ${packet.invitationId}")
                check(invitation.contactId == context.contactId) {
                    "Authorization revocation contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                    "Authorization revocation challenge does not match invitation"
                }
                check(invitation.remoteSigningPublicKey.contentEquals(packet.revokerSigningPublicKey)) {
                    "Authorization revocation signing key does not match the contact identity"
                }

                val payload =
                    authorizationPayloadEncoder.encodeRevoked(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        revokedAtEpochMilliseconds = packet.revokedAtEpochMilliseconds,
                        inviteChallenge = packet.inviteChallenge,
                        revokerSigningPublicKey = packet.revokerSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, packet.revokerSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.revokedAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Authorization revocation was created too far in the future"
                }

                if (invitation.stage == DirectIdentityExchangeStage.AUTHORIZATION_REVOKED.name) {
                    return@withLock
                }

                identityExchangeDao.upsert(
                    invitation.copy(
                        stage = DirectIdentityExchangeStage.AUTHORIZATION_REVOKED.name,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        lastError = null
                    )
                )
            }
        }

    private suspend fun rebindIncomingInvitation(
        invitation: IdentityExchangeEntity
    ): IdentityExchangeEntity {
        val remotePhoneNumber =
            invitation.remoteDisplayName
                ?.let { value -> phoneNumberNormalizer.normalize(value).getOrNull() }
        val contactId =
            resolveIncomingInviteContactId(
                resolvedContactId = invitation.contactId,
                remotePhoneNumber = remotePhoneNumber,
                remoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                remoteSigningPublicKey = invitation.remoteSigningPublicKey
            )
        if (contactId == invitation.contactId) {
            return invitation
        }

        stageIncomingInvitationIdentity(
            contactId = contactId,
            remoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
            remoteSigningPublicKey = invitation.remoteSigningPublicKey
        )
        remotePhoneNumber?.let { phoneNumber ->
            persistIncomingPhoneNumber(
                contactId = contactId,
                phoneNumber = phoneNumber,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            contactDao.usePhoneNumberAsDisplayNameWhenMissing(
                contactId = contactId,
                phoneNumber = phoneNumber,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        }

        return invitation
            .copy(
                contactId = contactId,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                lastError = null
            ).also { reboundInvitation ->
                identityExchangeDao.upsert(reboundInvitation)
            }
    }

    private suspend fun resolveIncomingInviteContactId(
        resolvedContactId: String,
        remotePhoneNumber: String?,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): String {
        val phoneContactId =
            remotePhoneNumber?.let { phoneNumber ->
                findEquivalentPhoneContactId(phoneNumber)
            }
        val identityContactId =
            contactDao
                .findBySigningPublicKey(remoteSigningPublicKey)
                ?.contact
                ?.id
        val targetContactId = phoneContactId ?: identityContactId ?: return resolvedContactId

        if (phoneContactId == null) {
            requireCompatiblePinnedIdentity(
                contactId = targetContactId,
                encryptionPublicKey = remoteEncryptionPublicKey,
                signingPublicKey = remoteSigningPublicKey
            )
        }

        if (targetContactId == resolvedContactId) {
            return targetContactId
        }

        if (
            resolvedContactId != targetContactId &&
            canMergeRoutingDuplicate(
                contactId = resolvedContactId,
                remotePhoneNumber = remotePhoneNumber,
                remoteEncryptionPublicKey = remoteEncryptionPublicKey,
                remoteSigningPublicKey = remoteSigningPublicKey
            )
        ) {
            mergeRoutingDuplicate(
                fromContactId = resolvedContactId,
                toContactId = targetContactId,
                moveBootstrapMapping = true
            )
        }

        if (
            identityContactId != null &&
            identityContactId != targetContactId &&
            identityContactId != resolvedContactId &&
            canMergeRoutingDuplicate(
                contactId = identityContactId,
                remotePhoneNumber = remotePhoneNumber,
                remoteEncryptionPublicKey = remoteEncryptionPublicKey,
                remoteSigningPublicKey = remoteSigningPublicKey
            )
        ) {
            mergeRoutingDuplicate(
                fromContactId = identityContactId,
                toContactId = targetContactId,
                moveBootstrapMapping = false
            )
        }

        val remainingIdentityContactId =
            contactDao
                .findBySigningPublicKey(remoteSigningPublicKey)
                ?.contact
                ?.id
        check(remainingIdentityContactId == null || remainingIdentityContactId == targetContactId) {
            "Contact identity is already assigned to another contact"
        }

        return targetContactId
    }

    private suspend fun findEquivalentPhoneContactId(phoneNumber: String): String? {
        val contacts = contactDao.observeAll().first()
        val matches =
            contacts.filter { contact ->
                contact.phoneNumbers.any { storedPhoneNumber ->
                    phoneNumbersEquivalent(
                        first = storedPhoneNumber.value,
                        second = phoneNumber
                    ) ||
                        phoneNumbersEquivalent(
                            first = storedPhoneNumber.normalizedValue,
                            second = phoneNumber
                        )
                }
            }

        if (matches.isEmpty()) {
            return null
        }

        return matches
            .firstOrNull { contact ->
                contact.contact.deviceContactLinkStatus == DEVICE_CONTACT_LINK_STATUS_LINKED ||
                    contact.contact.deviceContactId != null
            }?.contact
            ?.id
            ?: matches
                .firstOrNull { contact ->
                    contact.phoneNumbers.any { storedPhoneNumber ->
                        phoneNumberNormalizer
                            .normalize(storedPhoneNumber.normalizedValue)
                            .getOrNull() == phoneNumber
                    }
                }?.contact
                ?.id
            ?: matches.first().contact.id
    }

    private suspend fun canMergeRoutingDuplicate(
        contactId: String,
        remotePhoneNumber: String?,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ): Boolean {
        val contact = contactDao.findById(contactId) ?: return false
        if (
            contact.contact.deviceContactId != null ||
            contact.contact.deviceContactLinkStatus == DEVICE_CONTACT_LINK_STATUS_LINKED
        ) {
            return false
        }

        val identity = contact.publicIdentity
        if (
            identity != null &&
            (
                !identity.encryptionPublicKey.contentEquals(remoteEncryptionPublicKey) ||
                    !identity.signingPublicKey.contentEquals(remoteSigningPublicKey)
            )
        ) {
            return false
        }

        if (remotePhoneNumber == null) {
            return contact.phoneNumbers.isEmpty()
        }

        return contact.phoneNumbers.all { storedPhoneNumber ->
            phoneNumbersEquivalent(
                first = storedPhoneNumber.value,
                second = remotePhoneNumber
            ) ||
                phoneNumbersEquivalent(
                    first = storedPhoneNumber.normalizedValue,
                    second = remotePhoneNumber
                )
        }
    }

    private suspend fun mergeRoutingDuplicate(
        fromContactId: String,
        toContactId: String,
        moveBootstrapMapping: Boolean
    ) {
        if (moveBootstrapMapping) {
            moveBootstrapMapping(
                fromContactId = fromContactId,
                toContactId = toContactId
            )
        }
        identityExchangeDao.reassignContact(
            fromContactId = fromContactId,
            toContactId = toContactId
        )
        contactDao.deleteById(fromContactId)
    }

    private suspend fun moveBootstrapMapping(
        fromContactId: String,
        toContactId: String
    ) {
        val routingId = contactRoutingIdDao.findRoutingIdByContactId(fromContactId) ?: return
        if (!routingId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX)) {
            return
        }

        contactRoutingIdDao.deleteOtherContactMapping(
            routingId = routingId,
            contactId = toContactId
        )
        contactRoutingIdDao.upsert(
            ContactRoutingIdEntity(
                contactId = toContactId,
                routingId = routingId
            )
        )
    }

    private suspend fun prepareAcceptedRemoteIdentity(invitation: IdentityExchangeEntity) {
        contactKeyExchangeDataSource
            .prepareRemoteIdentityForHandshake(
                contactId = invitation.contactId,
                remoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                remoteSigningPublicKey = invitation.remoteSigningPublicKey
            )
    }

    private suspend fun stageIncomingInvitationIdentity(
        contactId: String,
        remoteEncryptionPublicKey: ByteArray,
        remoteSigningPublicKey: ByteArray
    ) {
        val existingIdentity = contactDao.findPublicIdentityByContactId(contactId)
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

        contactKeyExchangeDataSource
            .storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = remoteEncryptionPublicKey,
                signingPublicKey = remoteSigningPublicKey,
                origin = RemoteIdentityOrigin.REMOTE_HANDSHAKE
            )
    }

    private suspend fun requireCompatiblePinnedIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        val storedIdentity = contactDao.findPublicIdentityByContactId(contactId) ?: return
        if (
            storedIdentity.encryptionPublicKey.contentEquals(encryptionPublicKey) &&
            storedIdentity.signingPublicKey.contentEquals(signingPublicKey)
        ) {
            return
        }

        val identityIsPinned =
            storedIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name ||
                storedIdentity.verificationStatus == ContactVerificationStatus.VERIFIED.name
        check(!identityIsPinned) {
            "Contact identity changed; reset the contact before accepting new keys"
        }
    }

    private fun phoneNumbersEquivalent(
        first: String,
        second: String
    ): Boolean {
        val firstNormalized = phoneNumberNormalizer.normalize(first).getOrNull() ?: return false
        val secondNormalized = phoneNumberNormalizer.normalize(second).getOrNull() ?: return false
        if (firstNormalized == secondNormalized) {
            return true
        }

        val firstInternational = firstNormalized.startsWith('+')
        val secondInternational = secondNormalized.startsWith('+')
        if (firstInternational == secondInternational) {
            return false
        }

        val international = if (firstInternational) firstNormalized else secondNormalized
        val domestic = if (firstInternational) secondNormalized else firstNormalized
        val internationalDigits = international.filter { character -> character.isDigit() }
        val domesticDigits = domestic.filter { character -> character.isDigit() }
        val nationalDigits = domesticDigits.removePrefix("0")

        if (nationalDigits.length < MINIMUM_NATIONAL_NUMBER_DIGITS) {
            return false
        }
        if (!internationalDigits.endsWith(nationalDigits)) {
            return false
        }

        val countryCodeLength = internationalDigits.length - nationalDigits.length
        return countryCodeLength in MINIMUM_COUNTRY_CODE_DIGITS..MAXIMUM_COUNTRY_CODE_DIGITS
    }

    private suspend fun persistIncomingPhoneNumber(
        contactId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ) {
        val contact = contactDao.findById(contactId) ?: return
        if (
            contact.phoneNumbers.any { existing ->
                phoneNumbersEquivalent(
                    first = existing.value,
                    second = phoneNumber
                )
            }
        ) {
            return
        }

        if (contactDao.findByNormalizedPhoneNumber(phoneNumber) != null) {
            return
        }

        val phoneNumberId = IdGenerator.generate()
        contactDao.upsertPhoneNumbers(
            listOf(
                ContactPhoneNumberEntity(
                    id = phoneNumberId,
                    contactId = contactId,
                    value = phoneNumber,
                    normalizedValue = phoneNumber,
                    type = CONTACT_PHONE_NUMBER_TYPE_MOBILE,
                    label = null,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            )
        )
        if (contact.contact.preferredPhoneNumberId == null) {
            contactDao.upsertContact(
                contact.contact.copy(
                    preferredPhoneNumberId = phoneNumberId,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            )
        }
    }

    private suspend fun hasActiveDirectChatAuthorization(
        contactId: String,
        localIdentity: LocalPublicIdentity
    ): Boolean {
        val latestInvitation = identityExchangeDao.findLatestForContact(contactId)
        val latestAuthorizationEvent =
            identityExchangeDao.findLatestForContactByStages(
                contactId = contactId,
                stages = AUTHORIZATION_EVENT_STATES
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

        return state in DIRECT_CHAT_AUTHORIZED_STATES &&
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
            latestAuthorizationEvent?.stage == DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name &&
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
            invitation.direction == InvitationDirection.INCOMING.name &&
            (
                invitation.stage == DirectIdentityExchangeStage.ACCEPTANCE_SENT.name ||
                    invitation.stage == DirectIdentityExchangeStage.WAITING_FOR_READY.name
            )
        ) {
            queueAcceptanceReplay(invitation)
            return true
        }

        if (
            invitation.direction == InvitationDirection.OUTGOING.name &&
            invitation.stage == DirectIdentityExchangeStage.OUTGOING_CHALLENGE_SENT.name
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
            DirectIdentityExchangeStage.ACCEPTANCE_SENT.name,
            DirectIdentityExchangeStage.WAITING_FOR_READY.name ->
                queueAcceptanceReplay(invitation)

            DirectIdentityExchangeStage.CLOSED.name ->
                queueDecline(
                    contactId = invitation.contactId,
                    invitationId = invitation.exchangeId,
                    inviteChallenge = invitation.inviteChallenge
                )
        }
    }

    private suspend fun queueAcceptanceReplay(invitation: IdentityExchangeEntity) {
        prepareAcceptedRemoteIdentity(invitation)
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
        val packetId = acceptedPacketId(invitation.exchangeId)
        val payload =
            payloadEncoder.encodeAccepted(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = invitation.exchangeId,
                acceptedAtEpochMilliseconds = acceptedAt,
                profilePicture = profilePicture,
                inviteChallenge = invitation.inviteChallenge,
                responseChallenge = responseChallenge,
                inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                inviterSigningPublicKey = invitation.remoteSigningPublicKey,
                responderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                responderSigningPublicKey = localIdentity.signingPublicKey
            )
        val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
        enqueueOrResend(
            contactId = invitation.contactId,
            packet =
                ContactInviteAcceptedPacket(
                    packetId = packetId,
                    invitationId = invitation.exchangeId,
                    acceptedAtEpochMilliseconds = acceptedAt,
                    profilePicture = profilePicture,
                    inviteChallenge = invitation.inviteChallenge.copyOf(),
                    responseChallenge = responseChallenge.copyOf(),
                    inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey.copyOf(),
                    inviterSigningPublicKey = invitation.remoteSigningPublicKey.copyOf(),
                    responderEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                    responderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                    signature = signature.copyOf()
                )
        ).getOrThrow()
        contactKeyExchangeDataSource
            .markMutual(
                contactId = invitation.contactId,
                expectedRemoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                expectedRemoteSigningPublicKey = invitation.remoteSigningPublicKey
            )
        identityExchangeDao.upsert(
            invitation.copy(
                stage = DirectIdentityExchangeStage.WAITING_FOR_READY.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                lastError = null,
                localEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                localSigningPublicKey = localIdentity.signingPublicKey.copyOf()
            )
        )
    }

    private suspend fun queueReadyReplay(
        contactId: String,
        packet: ContactInviteAcceptedPacket
    ) {
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        requireLocalKeysMatch(localIdentity, signingKeyPair)
        val readyAt = SystemClock.nowEpochMilliseconds()
        val packetId = readyPacketId(packet.invitationId)
        val payload =
            payloadEncoder.encodeReady(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = packet.invitationId,
                readyAtEpochMilliseconds = readyAt,
                responseChallenge = packet.responseChallenge,
                acceptedResponderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                acceptedResponderSigningPublicKey = packet.responderSigningPublicKey,
                senderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                senderSigningPublicKey = localIdentity.signingPublicKey
            )
        val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
        enqueueOrResend(
            contactId = contactId,
            packet =
                ContactReadyPacket(
                    packetId = packetId,
                    invitationId = packet.invitationId,
                    readyAtEpochMilliseconds = readyAt,
                    responseChallenge = packet.responseChallenge.copyOf(),
                    acceptedResponderEncryptionPublicKey = packet.responderEncryptionPublicKey.copyOf(),
                    acceptedResponderSigningPublicKey = packet.responderSigningPublicKey.copyOf(),
                    senderEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                    senderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                    signature = signature.copyOf()
                )
        ).getOrThrow()
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
            DirectIdentityExchangeStage.ACCEPTANCE_SENT.name -> IdentityHandshakeState.ACCEPTANCE_SENT
            DirectIdentityExchangeStage.WAITING_FOR_READY.name -> IdentityHandshakeState.WAITING_FOR_READY
            DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name -> IdentityHandshakeState.MUTUAL_UNVERIFIED
            DirectIdentityExchangeStage.AUTHORIZATION_REVOKED.name -> IdentityHandshakeState.AUTHORIZATION_REVOKED
            DirectIdentityExchangeStage.FAILED.name -> IdentityHandshakeState.FAILED
            else -> null
        }

    private fun invitePacketId(invitationId: String): String = "contact-invite-$invitationId"

    private fun acceptedPacketId(invitationId: String): String = "contact-invite-accepted-$invitationId"

    private fun readyPacketId(invitationId: String): String = "contact-ready-$invitationId"

    private fun declinedPacketId(invitationId: String): String = "contact-invite-declined-$invitationId"

    private fun authorizationRevokedPacketId(invitationId: String): String =
        "direct-chat-authorization-revoked-$invitationId"

    private fun requirePacketId(
        actualPacketId: String,
        expectedPrefix: String,
        invitationId: String
    ) {
        check(actualPacketId == "$expectedPrefix-$invitationId") {
            "Packet ID does not match the invitation transition"
        }
    }

    private suspend fun requireInvitation(
        invitationId: String,
        direction: InvitationDirection
    ): IdentityExchangeEntity {
        require(invitationId.isNotBlank()) {
            "Invitation ID must not be blank"
        }

        val invitation = identityExchangeDao.findById(invitationId) ?: error("Invitation was not found: $invitationId")
        check(invitation.direction == direction.name) {
            "Invitation direction does not match this operation"
        }
        return invitation
    }

    private suspend fun ensureNotExpired(invitation: IdentityExchangeEntity) {
        if (invitation.expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
            return
        }

        identityExchangeDao.upsert(
            invitation.copy(
                stage = DirectIdentityExchangeStage.CLOSED.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                lastError = "Invitation expired"
            )
        )
        error("Invitation has expired")
    }

    private fun requireState(
        invitation: IdentityExchangeEntity,
        expectedState: DirectIdentityExchangeStage
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

    private suspend fun queueDirectChatAuthorizationRevocation(
        invitation: IdentityExchangeEntity
    ) {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val revokedAt = SystemClock.nowEpochMilliseconds()
        val packetId = authorizationRevokedPacketId(invitation.exchangeId)
        val payload =
            authorizationPayloadEncoder.encodeRevoked(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = invitation.exchangeId,
                revokedAtEpochMilliseconds = revokedAt,
                inviteChallenge = invitation.inviteChallenge,
                revokerSigningPublicKey = signingKeyPair.publicKey
            )
        val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()

        enqueueOrResend(
            contactId = invitation.contactId,
            packet =
                DirectChatAuthorizationRevokedPacket(
                    packetId = packetId,
                    invitationId = invitation.exchangeId,
                    revokedAtEpochMilliseconds = revokedAt,
                    inviteChallenge = invitation.inviteChallenge.copyOf(),
                    revokerSigningPublicKey = signingKeyPair.publicKey.copyOf(),
                    signature = signature.copyOf()
                )
        ).getOrThrow()
    }

    private suspend fun shouldAutomaticallyDecline(
        contactId: String,
        blockedPeerIds: Set<String>,
        blockUnknownPeers: Boolean
    ): Boolean {
        if (contactId in blockedPeerIds) {
            return true
        }
        if (!blockUnknownPeers) {
            return false
        }

        val contact = contactDao.findById(contactId) ?: return true
        return contact.contact.deviceContactId == null &&
            contact.phoneNumbers.isEmpty() &&
            contact.publicIdentity?.locallyImported != true
    }

    private suspend fun queueDecline(
        contactId: String,
        invitationId: String,
        inviteChallenge: ByteArray
    ) {
        val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val declinedAt = SystemClock.nowEpochMilliseconds()
        val packetId = declinedPacketId(invitationId)
        val payload =
            payloadEncoder.encodeDeclined(
                packetId = packetId,
                version = ProtocolVersion.CURRENT,
                invitationId = invitationId,
                declinedAtEpochMilliseconds = declinedAt,
                inviteChallenge = inviteChallenge,
                declinerSigningPublicKey = signingKeyPair.publicKey
            )
        val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()

        enqueueOrResend(
            contactId = contactId,
            packet =
                ContactInviteDeclinedPacket(
                    packetId = packetId,
                    invitationId = invitationId,
                    declinedAtEpochMilliseconds = declinedAt,
                    inviteChallenge = inviteChallenge.copyOf(),
                    declinerSigningPublicKey = signingKeyPair.publicKey.copyOf(),
                    signature = signature.copyOf()
                )
        ).getOrThrow()
    }

    private fun IdentityExchangeEntity.toLifecycleRecord(): InvitationLifecycleRecord =
        InvitationLifecycleRecord(
            invitationId = exchangeId,
            payloadType = InvitationPayloadType.DIRECT,
            payloadId = contactId,
            peerId = contactId,
            direction =
                InvitationDirection.entries.firstOrNull { direction -> direction.name == this.direction }
                    ?: error("Unknown invitation direction: $direction"),
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
            updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
        )

    private companion object {
        const val CONTACT_PHONE_NUMBER_TYPE_MOBILE = "MOBILE"
        const val DEVICE_CONTACT_LINK_STATUS_LINKED = "LINKED"
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
        const val CHALLENGE_SIZE = 32
        const val INVITATION_LIFETIME_MILLISECONDS = 24L * 60L * 60L * 1_000L
        const val INVITATION_RESTART_GRACE_MILLISECONDS = 5L * 1_000L
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
        const val MAXIMUM_COUNTRY_CODE_DIGITS = 3
        const val MINIMUM_COUNTRY_CODE_DIGITS = 1
        const val MINIMUM_NATIONAL_NUMBER_DIGITS = 7
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"

        val DIRECT_CHAT_AUTHORIZED_STATES =
            setOf(
                IdentityHandshakeState.WAITING_FOR_READY,
                IdentityHandshakeState.MUTUAL_UNVERIFIED
            )

        val AUTHORIZATION_EVENT_STATES =
            listOf(
                DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                DirectIdentityExchangeStage.AUTHORIZATION_REVOKED.name
            )

        val TERMINAL_STATES =
            listOf(
                DirectIdentityExchangeStage.MUTUAL_UNVERIFIED.name,
                DirectIdentityExchangeStage.CLOSED.name,
                DirectIdentityExchangeStage.AUTHORIZATION_REVOKED.name,
                DirectIdentityExchangeStage.FAILED.name
            )
    }
}
