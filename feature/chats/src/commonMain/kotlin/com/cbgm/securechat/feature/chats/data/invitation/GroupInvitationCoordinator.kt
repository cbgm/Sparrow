package com.cbgm.securechat.feature.chats.data.invitation

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.message.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.security.GroupWelcomeRecipient
import com.cbgm.securechat.feature.chats.data.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityOrigin
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupInvitationCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val getContact: GetContact,
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val groupInvitationManager: GroupInvitationManager,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator,
    private val groupVerificationDao: GroupVerificationDao
) {
    private val activationMutex = Mutex()

    suspend fun createGroup(
        title: String,
        contactIds: Set<String>
    ): Result<String> =
        runCatching {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Group title must not be blank" }
            require(contactIds.isNotEmpty()) { "A group requires at least one contact" }

            val contacts =
                contactIds
                    .map { contactId -> loadContact(contactId) }
                    .sortedBy(Contact::id)
            val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val groupId = IdGenerator.generate(prefix = "group")
            val expiresAt = now + INVITATION_VALIDITY_MILLISECONDS
            val conversation =
                ConversationEntity(
                    id = groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = normalizedTitle,
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )

            chatDao.upsertConversation(conversation)

            val invitationsAndPackets =
                contacts.map { contact ->
                    val invitationId = IdGenerator.generate(prefix = "group-invitation")
                    val packet =
                        groupInvitationManager
                            .createInvite(
                                invitationId = invitationId,
                                groupId = groupId,
                                title = normalizedTitle,
                                createdAtEpochMilliseconds = now,
                                expiresAtEpochMilliseconds = expiresAt,
                                ownerIdentity = ownerIdentity,
                                ownerSigningKeyPair = ownerSigningKeyPair
                            ).getOrThrow()
                    val entity =
                        GroupInvitationEntity(
                            invitationId = invitationId,
                            groupId = groupId,
                            contactId = contact.id,
                            direction = GroupInvitationDirection.OUTGOING.name,
                            status = GroupInvitationStatus.INVITE_SENT.name,
                            challenge = packet.challenge.copyOf(),
                            createdAtEpochMilliseconds = now,
                            expiresAtEpochMilliseconds = expiresAt,
                            updatedAtEpochMilliseconds = now
                        )

                    entity to packet
                }

            groupInvitationDao.upsertAll(invitationsAndPackets.map { (entity, _) -> entity })
            groupVerificationCoordinator
                .initializeOwnedGroup(groupId)
                .getOrThrow()

            invitationsAndPackets.forEach { (entity, packet) ->
                protocolOutbox.enqueue(entity.contactId, packet).getOrThrow()
            }
            groupId
        }

    suspend fun addMembers(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactIds.isNotEmpty()) { "Choose at least one contact" }

            activationMutex.withLock {
                val conversation =
                    chatDao.findConversationById(groupId)
                        ?: error("Group conversation was not found")
                check(conversation.type == GROUP_CONVERSATION_TYPE) {
                    "Conversation is not a group"
                }
                val currentInvitations = groupInvitationDao.findByGroupId(groupId)
                check(
                    currentInvitations.none { invitation ->
                        invitation.direction == GroupInvitationDirection.INCOMING.name
                    }
                ) {
                    "Only the group owner may add members"
                }
                groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()

                val activeOrPendingContactIds =
                    currentInvitations
                        .filterNot { invitation -> invitation.status.isTerminalStatus() }
                        .mapTo(mutableSetOf(), GroupInvitationEntity::contactId)
                check(contactIds.none(activeOrPendingContactIds::contains)) {
                    "One or more contacts already belong to this group"
                }

                val contacts =
                    contactIds
                        .map { contactId -> loadContact(contactId) }
                        .sortedBy(Contact::id)
                val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val now = SystemClock.nowEpochMilliseconds()
                val expiresAt = now + INVITATION_VALIDITY_MILLISECONDS
                val invitationsAndPackets =
                    contacts.map { contact ->
                        groupInvitationDao.deleteByGroupAndContact(groupId, contact.id)
                        val invitationId = IdGenerator.generate(prefix = "group-invitation")
                        val packet =
                            groupInvitationManager
                                .createInvite(
                                    invitationId = invitationId,
                                    groupId = groupId,
                                    title = requireNotNull(conversation.title),
                                    createdAtEpochMilliseconds = now,
                                    expiresAtEpochMilliseconds = expiresAt,
                                    ownerIdentity = ownerIdentity,
                                    ownerSigningKeyPair = ownerSigningKeyPair
                                ).getOrThrow()

                        GroupInvitationEntity(
                            invitationId = invitationId,
                            groupId = groupId,
                            contactId = contact.id,
                            direction = GroupInvitationDirection.OUTGOING.name,
                            status = GroupInvitationStatus.INVITE_SENT.name,
                            challenge = packet.challenge.copyOf(),
                            createdAtEpochMilliseconds = now,
                            expiresAtEpochMilliseconds = expiresAt,
                            updatedAtEpochMilliseconds = now
                        ) to packet
                    }

                groupInvitationDao.upsertAll(invitationsAndPackets.map { (entity, _) -> entity })
                groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
                invitationsAndPackets.forEach { (entity, packet) ->
                    protocolOutbox.enqueue(entity.contactId, packet).getOrThrow()
                }
                chatDao.updateConversationTimestamp(groupId, now)
            }
        }

    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            activationMutex.withLock {
                removeMemberLocked(
                    groupId = groupId,
                    contactId = contactId,
                    reason = GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER
                )
            }
        }

    suspend fun leaveGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }

            activationMutex.withLock {
                val invitation = requireIncomingInvitation(groupId)
                check(invitation.status == GroupInvitationStatus.ACTIVE.name) {
                    "Only an active group member may leave the group"
                }
                val epoch =
                    groupSecurityManager
                        .findJoinedGroupEpoch(
                            groupId = groupId,
                            ownerContactId = invitation.contactId
                        ).getOrThrow()
                        ?: error("Active group security state was not found")
                val hasHistory = chatDao.hasMessages(groupId)
                val requestedAt =
                    maxOf(
                        invitation.createdAtEpochMilliseconds,
                        SystemClock.nowEpochMilliseconds()
                    )
                val leaveRequest =
                    groupInvitationManager
                        .createLeaveRequest(
                            invitationId = invitation.invitationId,
                            groupId = groupId,
                            epoch = epoch,
                            challenge = invitation.challenge,
                            requestedAtEpochMilliseconds = requestedAt,
                            memberSigningKeyPair =
                                localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                        ).getOrThrow()
                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = GroupInvitationStatus.ACTIVE.name,
                        newStatus = GroupInvitationStatus.LEAVE_SENT.name,
                        updatedAt = requestedAt
                    )
                check(updated == 1) { "Group membership changed before the leave request was queued" }

                protocolOutbox
                    .enqueue(invitation.contactId, leaveRequest)
                    .onFailure {
                        groupInvitationDao.updateStatus(
                            invitationId = invitation.invitationId,
                            expectedStatus = GroupInvitationStatus.LEAVE_SENT.name,
                            newStatus = GroupInvitationStatus.ACTIVE.name,
                            updatedAt = requestedAt
                        )
                    }.getOrThrow()
                if (!hasHistory) {
                    hideEmptyJoinedGroupConversation(
                        groupId = groupId,
                        hiddenAtEpochMilliseconds = requestedAt
                    )
                }
            }
        }

    suspend fun deleteGroupConversation(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val invitations = groupInvitationDao.findByGroupId(groupId)
            val isOwner =
                groupSecurityManager.isOwnedGroup(groupId).getOrThrow()
                    ?: invitations.any { invitation ->
                        invitation.direction == GroupInvitationDirection.OUTGOING.name
                    }

            if (isOwner) {
                deleteOwnedGroupConversation(groupId, invitations)
            } else {
                deleteJoinedGroupConversation(groupId, invitations)
            }
        }

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket
    ): Result<Unit> =
        runCatching {
            activationMutex.withLock {
                val invitation =
                    groupInvitationDao.findByInvitationId(packet.invitationId)
                        ?: error("Leaving group member was not found")
                check(invitation.groupId == packet.groupId) {
                    "Group leave request references the wrong group"
                }
                check(invitation.contactId == memberContactId) {
                    "Group leave request came from a different member"
                }
                check(invitation.challenge.contentEquals(packet.challenge)) {
                    "Group leave request invitation challenge does not match"
                }
                val memberIdentity =
                    loadContact(memberContactId).secureChatIdentity
                        ?: error("Leaving group member identity was not found")
                check(memberIdentity.signingPublicKey.contentEquals(packet.memberSigningPublicKey)) {
                    "Group leave request signing identity does not match the member"
                }
                groupInvitationManager
                    .verifyLeaveRequest(
                        packet = packet,
                        expectedMemberSigningPublicKey = memberIdentity.signingPublicKey
                    ).getOrThrow()

                if (invitation.status == GroupInvitationStatus.REMOVED.name) {
                    return@withLock
                }
                check(invitation.status == GroupInvitationStatus.ACTIVE.name) {
                    "Only an active group member may leave the group"
                }
                val currentEpoch =
                    groupSecurityManager.findOwnedGroupEpoch(packet.groupId).getOrThrow()
                        ?: error("Active group security state was not found")
                check(packet.epoch <= currentEpoch) {
                    "Group leave request references a future group epoch"
                }

                removeMemberLocked(
                    groupId = packet.groupId,
                    contactId = memberContactId,
                    reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT
                )
            }
        }

    suspend fun receiveInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            groupInvitationManager.verifyInvite(packet).getOrThrow()
            val localDeletionTimestamp =
                chatDao.findMessageTimestampByTransportMode(
                    conversationId = packet.groupId,
                    transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
                )
            if (localDeletionTimestamp != null) {
                if (packet.createdAtEpochMilliseconds <= localDeletionTimestamp) {
                    return@runCatching
                }
                chatDao.deleteConversationMessages(packet.groupId)
            }
            val persistedAtEpochMilliseconds =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                )

            val existingInvitation = groupInvitationDao.findByInvitationId(packet.invitationId)
            if (existingInvitation != null) {
                check(
                    existingInvitation.groupId == packet.groupId &&
                        existingInvitation.contactId == ownerContactId &&
                        existingInvitation.challenge.contentEquals(packet.challenge)
                ) {
                    "Group invitation conflicts with an existing invitation"
                }
                return@runCatching
            }
            val replacedInvitation =
                groupInvitationDao.findByGroupAndContact(packet.groupId, ownerContactId)
            if (replacedInvitation != null) {
                check(replacedInvitation.status.isTerminalStatus()) {
                    "A current group invitation already exists for this group"
                }
            }

            val replacesUntrustedIdentity =
                validateContactIdentity(
                    contactId = ownerContactId,
                    encryptionPublicKey = packet.ownerEncryptionPublicKey,
                    signingPublicKey = packet.ownerSigningPublicKey
                )
            if (replacesUntrustedIdentity) {
                groupInvitationDao.failSupersededIncomingInvitations(
                    contactId = ownerContactId,
                    currentInvitationId = packet.invitationId,
                    awaitingAcceptanceStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    failedStatus = GroupInvitationStatus.FAILED.name,
                    updatedAt = persistedAtEpochMilliseconds
                )
            }
            storeRemoteIdentity(
                contactId = ownerContactId,
                encryptionPublicKey = packet.ownerEncryptionPublicKey,
                signingPublicKey = packet.ownerSigningPublicKey
            )

            chatDao.upsertConversation(
                ConversationEntity(
                    id = packet.groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = packet.title,
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = persistedAtEpochMilliseconds
                )
            )
            val invitation =
                GroupInvitationEntity(
                    invitationId = packet.invitationId,
                    groupId = packet.groupId,
                    contactId = ownerContactId,
                    direction = GroupInvitationDirection.INCOMING.name,
                    status = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    challenge = packet.challenge.copyOf(),
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = persistedAtEpochMilliseconds
                )
            if (replacedInvitation == null) {
                groupInvitationDao.upsert(invitation)
            } else {
                groupInvitationDao.replaceForGroupAndContact(invitation)
            }
        }

    suspend fun acceptInvitation(groupId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncomingInvitation(groupId)
            check(invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name) {
                "Group invitation cannot be accepted from status ${invitation.status}"
            }
            val now = SystemClock.nowEpochMilliseconds()
            if (now > invitation.expiresAtEpochMilliseconds) {
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = invitation.status,
                    newStatus = GroupInvitationStatus.EXPIRED.name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = now
                        )
                )
                error("Group invitation has expired")
            }

            val ownerIdentity =
                loadContact(invitation.contactId).secureChatIdentity
                    ?: error("Group owner identity was not stored")
            if (ownerIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
                contactKeyExchangeStore
                    .acceptRemoteIdentityForHandshake(
                        contactId = invitation.contactId,
                        expectedRemoteEncryptionPublicKey = ownerIdentity.encryptionPublicKey,
                        expectedRemoteSigningPublicKey = ownerIdentity.signingPublicKey
                    ).getOrThrow()
            }
            val memberIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val joinRequest =
                groupInvitationManager
                    .createJoinRequest(
                        invitationId = invitation.invitationId,
                        groupId = invitation.groupId,
                        challenge = invitation.challenge,
                        memberIdentity = memberIdentity,
                        memberSigningKeyPair = memberSigningKeyPair
                    ).getOrThrow()

            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    newStatus = GroupInvitationStatus.JOIN_SENT.name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = now
                        )
                )
            check(updated == 1) { "Group invitation changed while it was accepted" }

            protocolOutbox.enqueue(invitation.contactId, joinRequest).getOrElse { error ->
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                    newStatus = GroupInvitationStatus.FAILED.name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                )
                throw error
            }
        }

    suspend fun declineInvitation(groupId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncomingInvitation(groupId)
            check(invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name) {
                "Group invitation cannot be declined from status ${invitation.status}"
            }
            val hasHistory = chatDao.hasMessages(groupId)
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet =
                groupInvitationManager
                    .createDecline(
                        invitationId = invitation.invitationId,
                        groupId = invitation.groupId,
                        challenge = invitation.challenge,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            protocolOutbox.enqueue(invitation.contactId, packet).getOrThrow()
            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    newStatus = GroupInvitationStatus.DECLINED.name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                )
            check(updated == 1) { "Group invitation changed while it was declined" }
            if (!hasHistory) {
                hideEmptyJoinedGroupConversation(
                    groupId = groupId,
                    hiddenAtEpochMilliseconds =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                )
            }
        }

    suspend fun receiveJoinRequest(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: error("Group invitation was not found")

            check(invitation.groupId == packet.groupId) { "Join request uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Join request came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Join request challenge does not match" }
            check(receivedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                "Group invitation has expired"
            }

            groupInvitationManager.verifyJoinRequest(packet).getOrThrow()

            if (
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                invitation.status == GroupInvitationStatus.ACTIVE.name
            ) {
                return@runCatching
            }

            validateContactIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            )
            storeMutualIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            )

            if (
                invitation.status == GroupInvitationStatus.INVITE_SENT.name ||
                invitation.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name
            ) {
                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = invitation.status,
                        newStatus = GroupInvitationStatus.IDENTITY_READY.name,
                        updatedAt =
                            resolveInvitationUpdatedAt(
                                createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                                candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                            )
                    )
                check(updated == 1) { "Group invitation changed while the join request was applied" }
            } else {
                check(invitation.status == GroupInvitationStatus.IDENTITY_READY.name) {
                    "Unsupported group invitation status: ${invitation.status}"
                }
            }

            activateGroupIfReady(packet.groupId).getOrThrow()
        }

    suspend fun receiveDecline(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: error("Group invitation was not found")
            check(invitation.groupId == packet.groupId) { "Decline uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Decline came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Decline challenge does not match" }
            groupInvitationManager.verifyDecline(packet).getOrThrow()
            ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)

            if (
                invitation.status == GroupInvitationStatus.DECLINED.name ||
                invitation.status == GroupInvitationStatus.REMOVED.name
            ) {
                return@runCatching
            }
            if (
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                invitation.status == GroupInvitationStatus.ACTIVE.name
            ) {
                activationMutex.withLock {
                    removeMemberLocked(
                        groupId = packet.groupId,
                        contactId = memberContactId,
                        reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT
                    )
                }
                return@runCatching
            }
            check(
                invitation.status == GroupInvitationStatus.INVITE_SENT.name ||
                    invitation.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name ||
                    invitation.status == GroupInvitationStatus.IDENTITY_READY.name
            ) {
                "Group invitation cannot be declined after it was accepted"
            }
            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = invitation.status,
                    newStatus = GroupInvitationStatus.DECLINED.name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                        )
                )
            check(updated == 1) { "Group invitation changed while the decline was applied" }
            groupVerificationCoordinator
                .onOwnedMembershipChanged(packet.groupId)
                .getOrThrow()
        }

    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            activationMutex.withLock {
                val invitation =
                    groupInvitationDao.findByGroupAndContact(packet.groupId, memberContactId)
                        ?: error("Group invitation was not found")
                val expectedIdentity =
                    loadContact(memberContactId).secureChatIdentity
                        ?: error("Group member identity was not found")
                check(
                    packet.welcomePacketId ==
                        groupSecurityManager.welcomePacketId(
                            groupId = packet.groupId,
                            invitationId = invitation.invitationId,
                            epoch = packet.epoch
                        )
                ) {
                    "Ready acknowledgement references the wrong welcome"
                }
                groupInvitationManager
                    .verifyReadyAcknowledgement(packet, expectedIdentity.signingPublicKey)
                    .getOrThrow()
                groupSecurityManager
                    .verifyKeyConfirmation(
                        groupId = packet.groupId,
                        epoch = packet.epoch,
                        keyConfirmation = packet.keyConfirmation
                    ).getOrThrow()

                if (invitation.status == GroupInvitationStatus.ACTIVE.name) {
                    return@withLock
                }
                check(invitation.status == GroupInvitationStatus.WELCOME_SENT.name) {
                    "Group member is not waiting for a ready acknowledgement"
                }

                val activatedContact = loadContact(memberContactId)
                val activeContacts =
                    groupInvitationDao
                        .findByGroupId(packet.groupId)
                        .filter { activeInvitation -> activeInvitation.status == GroupInvitationStatus.ACTIVE.name }
                        .map { activeInvitation -> loadContact(activeInvitation.contactId) }
                        .filterNot { contact -> contact.id == memberContactId }
                        .sortedBy(Contact::id)
                val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val activationTimestamp =
                    maxOf(
                        invitation.createdAtEpochMilliseconds,
                        receivedAtEpochMilliseconds
                    )
                val shouldRecordMemberAdded = chatDao.hasMessages(packet.groupId)

                activeContacts.forEach { activeContact ->
                    enqueueMemberActivation(
                        groupId = packet.groupId,
                        epoch = packet.epoch,
                        activationId = packet.packetId,
                        activatedAtEpochMilliseconds = activationTimestamp,
                        activationRound = GroupMemberActivatedPacket.DISCOVERY_ROUND,
                        memberContact = activatedContact,
                        recipientContactId = activeContact.id,
                        ownerSigningKeyPair = ownerSigningKeyPair
                    )
                }
                enqueueMemberActivation(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    activationId = packet.packetId,
                    activatedAtEpochMilliseconds = activationTimestamp,
                    activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                    memberContact = activatedContact,
                    recipientContactId = memberContactId,
                    ownerSigningKeyPair = ownerSigningKeyPair
                )

                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = GroupInvitationStatus.WELCOME_SENT.name,
                        newStatus = GroupInvitationStatus.ACTIVE.name,
                        updatedAt = activationTimestamp
                    )
                check(updated == 1) { "Group invitation changed while readiness was applied" }

                chatDao.upsertConversationParticipant(
                    ConversationParticipantEntity(
                        conversationId = packet.groupId,
                        contactId = memberContactId,
                        role = GROUP_MEMBER_ROLE,
                        joinedAtEpochMilliseconds = activationTimestamp
                    )
                )
                if (shouldRecordMemberAdded) {
                    chatDao.upsertMessage(
                        GroupMembershipMessageFactory.memberAdded(
                            conversationId = packet.groupId,
                            epoch = packet.epoch,
                            contactId = memberContactId,
                            contactName = activatedContact.membershipDisplayName(),
                            createdAtEpochMilliseconds = activationTimestamp,
                            eventId = invitation.invitationId
                        )
                    )
                    chatDao.updateConversationTimestamp(packet.groupId, activationTimestamp)
                }

                groupVerificationCoordinator
                    .onOwnedMembershipChanged(packet.groupId)
                    .getOrThrow()
            }
        }

    suspend fun activateGroupIfReady(groupId: String): Result<Unit> =
        runCatching {
            activationMutex.withLock {
                val readyInvitations =
                    groupInvitationDao
                        .findByGroupId(groupId)
                        .filter { invitation -> invitation.status == GroupInvitationStatus.IDENTITY_READY.name }
                        .sortedBy(GroupInvitationEntity::invitationId)

                readyInvitations.forEach { invitation ->
                    distributeGroupKeyToMember(groupId, invitation)
                }
            }
        }

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String
    ): Result<Unit> =
        runCatching {
            activationMutex.withLock {
                val acknowledgingInvitation =
                    groupInvitationDao.findByGroupAndContact(packet.groupId, acknowledgingContactId)
                        ?: error("Acknowledging group member was not found")
                check(acknowledgingInvitation.status == GroupInvitationStatus.ACTIVE.name) {
                    "Only an active group member may acknowledge another member"
                }

                val activatedContact =
                    groupInvitationDao
                        .findByGroupId(packet.groupId)
                        .filter { invitation -> invitation.status == GroupInvitationStatus.ACTIVE.name }
                        .map { invitation -> loadContact(invitation.contactId) }
                        .singleOrNull { contact ->
                            contact.secureChatIdentity
                                ?.signingPublicKey
                                ?.contentEquals(packet.activatedMemberSigningPublicKey) == true
                        } ?: error("Activated group member was not found")
                check(activatedContact.id != acknowledgingContactId) {
                    "A group member cannot acknowledge its own activation"
                }

                val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val acknowledgingContact = loadContact(acknowledgingContactId)

                when (packet.activationRound) {
                    GroupMemberActivatedPacket.DISCOVERY_ROUND ->
                        enqueueMemberActivation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            activatedAtEpochMilliseconds = acknowledgingInvitation.updatedAtEpochMilliseconds,
                            activationRound = GroupMemberActivatedPacket.RECIPROCAL_ROUND,
                            memberContact = acknowledgingContact,
                            recipientContactId = activatedContact.id,
                            ownerSigningKeyPair = ownerSigningKeyPair
                        )

                    GroupMemberActivatedPacket.RECIPROCAL_ROUND -> {
                        enqueueMemberActivation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            activatedAtEpochMilliseconds =
                                groupInvitationDao
                                    .findByGroupAndContact(packet.groupId, activatedContact.id)
                                    ?.updatedAtEpochMilliseconds
                                    ?: packet.acknowledgedAtEpochMilliseconds,
                            activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                            memberContact = activatedContact,
                            recipientContactId = acknowledgingContactId,
                            ownerSigningKeyPair = ownerSigningKeyPair
                        )
                        enqueueMemberActivation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            activatedAtEpochMilliseconds = acknowledgingInvitation.updatedAtEpochMilliseconds,
                            activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                            memberContact = acknowledgingContact,
                            recipientContactId = activatedContact.id,
                            ownerSigningKeyPair = ownerSigningKeyPair
                        )
                    }

                    else -> error("Unsupported member activation acknowledgement round")
                }
            }
        }

    private suspend fun distributeGroupKeyToMember(
        groupId: String,
        invitation: GroupInvitationEntity
    ) {
        val conversation = chatDao.findConversationById(groupId) ?: error("Pending group was not found")
        val contact = loadContact(invitation.contactId)
        check(contact.hasMutualIdentity()) { "Group member identity is not ready: ${contact.id}" }

        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val activeContacts =
            groupInvitationDao
                .findByGroupId(groupId)
                .filter { existing -> existing.status == GroupInvitationStatus.ACTIVE.name }
                .map { existing -> loadContact(existing.contactId) }
        val members =
            (activeContacts + contact)
                .distinctBy(Contact::id)
                .sortedBy(Contact::id)
        val currentEpoch = groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        val targetEpoch = currentEpoch?.plus(1) ?: INITIAL_GROUP_EPOCH
        val securedGroup =
            if (currentEpoch == null) {
                groupSecurityManager.createOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    memberPayloads = createMemberPayloads(localIdentity, localPhoneNumber, members),
                    memberKeys = createMemberKeys(groupId, targetEpoch, members),
                    recipients = createRecipients(groupId, members),
                    localSigningKeyPair = localSigningKeyPair
                )
            } else {
                groupSecurityManager.rotateOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    memberPayloads = createMemberPayloads(localIdentity, localPhoneNumber, members),
                    memberKeys = createMemberKeys(groupId, targetEpoch, members),
                    recipients = createRecipients(groupId, members),
                    localSigningKeyPair = localSigningKeyPair
                )
            }.getOrThrow()
        check(contact.id in securedGroup.welcomePacketsByContactId) {
            "Recipient welcome packet was not created"
        }

        securedGroup.welcomePacketsByContactId.forEach { (recipientContactId, packet) ->
            protocolOutbox.enqueue(recipientContactId, packet).getOrThrow()
        }

        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = GroupInvitationStatus.IDENTITY_READY.name,
                newStatus = GroupInvitationStatus.WELCOME_SENT.name,
                updatedAt = maxOf(invitation.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
            )
        check(updated == 1) { "Group invitation changed while its welcome was recorded" }
    }

    private suspend fun enqueueMemberActivation(
        groupId: String,
        epoch: Int,
        activationId: String,
        activatedAtEpochMilliseconds: Long,
        activationRound: Int,
        memberContact: Contact,
        recipientContactId: String,
        ownerSigningKeyPair: LocalSigningKeyPair
    ) {
        val identity =
            memberContact.secureChatIdentity
                ?: error("Activated group member has no SecureChat identity")
        val packet =
            groupInvitationManager
                .createMemberActivated(
                    groupId = groupId,
                    epoch = epoch,
                    member =
                        GroupMemberPayload(
                            displayName = memberContact.displayName,
                            encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                            signingPublicKey = identity.signingPublicKey.copyOf(),
                            role = GROUP_MEMBER_ROLE,
                            phoneNumber = memberContact.requirePhoneNumber()
                        ),
                    activatedAtEpochMilliseconds = activatedAtEpochMilliseconds,
                    activationRound = activationRound,
                    activationId = activationId,
                    memberReferenceId = memberContact.id,
                    recipientContactId = recipientContactId,
                    ownerSigningKeyPair = ownerSigningKeyPair
                ).getOrThrow()

        protocolOutbox.enqueue(recipientContactId, packet).getOrThrow()
    }

    private suspend fun loadContact(contactId: String): Contact = getContact(contactId).getOrThrow() ?: error("Contact was not found: $contactId")

    private suspend fun validateContactIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Boolean =
        InvitationIdentityPolicy.requiresReplacement(
            existing = loadContact(contactId).secureChatIdentity,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey
        )

    private suspend fun storeMutualIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        contactKeyExchangeStore
            .storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                origin = RemoteIdentityOrigin.CONTACT_INVITATION
            ).getOrThrow()
        val storedIdentity =
            loadContact(contactId).secureChatIdentity
                ?: error("Contact identity was not stored")
        if (storedIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            contactKeyExchangeStore
                .acceptRemoteIdentityForHandshake(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        }
        contactKeyExchangeStore
            .markMutual(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                expectedRemoteSigningPublicKey = signingPublicKey
            ).getOrThrow()
    }

    private suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        contactKeyExchangeStore
            .storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                origin = RemoteIdentityOrigin.CONTACT_INVITATION
            ).getOrThrow()
    }

    private suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ) {
        val existing = loadContact(contactId).secureChatIdentity ?: return
        check(existing.signingPublicKey.contentEquals(signingPublicKey)) {
            "Contact signing identity conflicts with the invitation response"
        }
    }

    private suspend fun requireIncomingInvitation(groupId: String): GroupInvitationEntity {
        val invitations = groupInvitationDao.findByGroupId(groupId)
        return invitations
            .singleOrNull { invitation ->
                invitation.direction == GroupInvitationDirection.INCOMING.name
            } ?: error("Incoming group invitation was not found")
    }

    private fun Contact.hasMutualIdentity(): Boolean {
        val identity = secureChatIdentity ?: return false
        return identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL &&
            identity.encryptionPublicKey.isNotEmpty() &&
            identity.signingPublicKey.isNotEmpty()
    }

    private fun createMemberPayloads(
        localIdentity: LocalPublicIdentity,
        localPhoneNumber: String,
        contacts: List<Contact>
    ): List<GroupMemberPayload> =
        buildList {
            add(
                GroupMemberPayload(
                    displayName = null,
                    encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                    signingPublicKey = localIdentity.signingPublicKey.copyOf(),
                    role = GROUP_OWNER_ROLE,
                    phoneNumber = localPhoneNumber
                )
            )
            contacts.forEach { contact ->
                val identity = requireNotNull(contact.secureChatIdentity)
                add(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                        signingPublicKey = identity.signingPublicKey.copyOf(),
                        role = GROUP_MEMBER_ROLE,
                        phoneNumber = contact.requirePhoneNumber()
                    )
                )
            }
        }

    private fun createMemberKeys(
        groupId: String,
        epoch: Int,
        contacts: List<Contact>
    ): List<GroupMemberKeyEntity> =
        contacts.map { contact ->
            val identity = requireNotNull(contact.secureChatIdentity)
            GroupMemberKeyEntity(
                groupId = groupId,
                epoch = epoch,
                contactId = contact.id,
                encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                signingPublicKey = identity.signingPublicKey.copyOf(),
                role = GROUP_MEMBER_ROLE
            )
        }

    private suspend fun removeMemberLocked(
        groupId: String,
        contactId: String,
        reason: String
    ) {
        val invitation =
            groupInvitationDao.findByGroupAndContact(groupId, contactId)
                ?: error("Group member was not found")
        check(invitation.direction == GroupInvitationDirection.OUTGOING.name) {
            "Only the group owner may remove members"
        }
        groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        check(!invitation.status.isTerminalStatus()) {
            "Group member is already inactive"
        }

        val now =
            maxOf(
                invitation.createdAtEpochMilliseconds,
                SystemClock.nowEpochMilliseconds()
            )
        val removedContact = loadContact(contactId)
        val membershipChange =
            if (reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                GroupMembershipChangePayload(
                    reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                    memberSigningPublicKey =
                        requireNotNull(removedContact.secureChatIdentity)
                            .signingPublicKey
                            .copyOf()
                )
            } else {
                null
            }
        val removalEpoch =
            if (
                invitation.status == GroupInvitationStatus.ACTIVE.name ||
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name
            ) {
                rotateAfterRemoval(
                    groupId = groupId,
                    removedContactId = contactId,
                    updatedAtEpochMilliseconds = now,
                    membershipChange = membershipChange
                )
            } else {
                GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH
            }
        val removalPacket =
            groupInvitationManager
                .createMemberRemoved(
                    invitationId = invitation.invitationId,
                    groupId = groupId,
                    epoch = removalEpoch,
                    reason = reason,
                    challenge = invitation.challenge,
                    removedMemberSigningPublicKey =
                        removedContact.secureChatIdentity
                            ?.signingPublicKey
                            ?.copyOf()
                            ?: byteArrayOf(),
                    removedAtEpochMilliseconds = now,
                    ownerSigningKeyPair =
                        localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
        protocolOutbox.enqueue(contactId, removalPacket).getOrThrow()

        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = invitation.status,
                newStatus = GroupInvitationStatus.REMOVED.name,
                updatedAt = now
            )
        check(updated == 1) { "Group membership changed while the member was removed" }
        chatDao.deleteConversationParticipant(groupId, contactId)
        chatDao.upsertMessage(
            if (reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                GroupMembershipMessageFactory.memberLeft(
                    conversationId = groupId,
                    epoch = removalEpoch,
                    contactId = contactId,
                    contactName = removedContact.membershipDisplayName(),
                    createdAtEpochMilliseconds = now,
                    eventId = invitation.invitationId
                )
            } else {
                GroupMembershipMessageFactory.memberRemoved(
                    conversationId = groupId,
                    epoch = removalEpoch,
                    contactId = contactId,
                    contactName = removedContact.membershipDisplayName(),
                    createdAtEpochMilliseconds = now,
                    eventId = invitation.invitationId
                )
            }
        )
        groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
        chatDao.updateConversationTimestamp(groupId, now)
    }

    private suspend fun rotateAfterRemoval(
        groupId: String,
        removedContactId: String,
        updatedAtEpochMilliseconds: Long,
        membershipChange: GroupMembershipChangePayload?
    ): Int {
        val conversation =
            chatDao.findConversationById(groupId)
                ?: error("Group conversation was not found")
        val currentEpoch =
            groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                ?: error("Active group security state was not found")
        val remainingContacts =
            groupInvitationDao
                .findByGroupId(groupId)
                .filter { invitation ->
                    invitation.status == GroupInvitationStatus.ACTIVE.name &&
                        invitation.contactId != removedContactId
                }.map { invitation -> loadContact(invitation.contactId) }
                .sortedBy(Contact::id)
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val nextEpoch = currentEpoch + 1
        val securedGroup =
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
                    memberPayloads =
                        createMemberPayloads(
                            localIdentity = localIdentity,
                            localPhoneNumber = localPhoneNumber,
                            contacts = remainingContacts
                        ),
                    memberKeys = createMemberKeys(groupId, nextEpoch, remainingContacts),
                    recipients = createRecipients(groupId, remainingContacts),
                    localSigningKeyPair = localSigningKeyPair,
                    membershipChange = membershipChange
                ).getOrThrow()

        securedGroup.welcomePacketsByContactId.forEach { (recipientContactId, packet) ->
            protocolOutbox.enqueue(recipientContactId, packet).getOrThrow()
        }
        return nextEpoch
    }

    private fun String.isIncomingStatus(): Boolean =
        this == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
            this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name ||
            this == GroupInvitationStatus.LEAVE_SENT.name

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.EXPIRED.name ||
            this == GroupInvitationStatus.FAILED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.GROUP_DELETED.name

    private suspend fun deleteOwnedGroupConversation(
        groupId: String,
        invitations: List<GroupInvitationEntity>
    ) {
        activationMutex.withLock {
            val now =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    invitations.maxOfOrNull(GroupInvitationEntity::createdAtEpochMilliseconds) ?: 0L
                )
            val epoch =
                groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
                    ?: GroupConversationDeletedPacket.PENDING_GROUP_EPOCH
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()

            invitations
                .filterNot { invitation -> invitation.status.isTerminalStatus() }
                .forEach { invitation ->
                    val packet =
                        groupInvitationManager
                            .createConversationDeleted(
                                invitationId = invitation.invitationId,
                                groupId = groupId,
                                epoch = epoch,
                                challenge = invitation.challenge,
                                deletedAtEpochMilliseconds = now,
                                ownerSigningKeyPair = signingKeyPair
                            ).getOrThrow()
                    protocolOutbox.enqueue(invitation.contactId, packet).getOrThrow()
                }

            deleteLocalGroupData(groupId, now)
        }
    }

    private suspend fun deleteJoinedGroupConversation(
        groupId: String,
        invitations: List<GroupInvitationEntity>
    ) {
        val invitation =
            invitations
                .filter { candidate ->
                    candidate.direction == GroupInvitationDirection.INCOMING.name &&
                        (
                            candidate.status.isIncomingStatus() ||
                                candidate.status == GroupInvitationStatus.ACTIVE.name
                        )
                }.maxByOrNull(GroupInvitationEntity::updatedAtEpochMilliseconds)
        if (invitation != null) {
            when (invitation.status) {
                GroupInvitationStatus.ACTIVE.name -> leaveGroup(groupId).getOrThrow()
                GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                GroupInvitationStatus.JOIN_SENT.name,
                GroupInvitationStatus.WAITING_FOR_ACTIVATION.name -> {
                    val decline =
                        groupInvitationManager
                            .createDecline(
                                invitationId = invitation.invitationId,
                                groupId = groupId,
                                challenge = invitation.challenge,
                                memberSigningKeyPair =
                                    localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                            ).getOrThrow()
                    protocolOutbox.enqueue(invitation.contactId, decline).getOrThrow()
                }
            }
        }

        deleteLocalGroupData(
            groupId = groupId,
            deletedAtEpochMilliseconds =
                maxOf(
                    SystemClock.nowEpochMilliseconds(),
                    invitations.maxOfOrNull(GroupInvitationEntity::createdAtEpochMilliseconds) ?: 0L
                )
        )
    }

    private suspend fun hideEmptyJoinedGroupConversation(
        groupId: String,
        hiddenAtEpochMilliseconds: Long
    ) {
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = hiddenAtEpochMilliseconds
            )
        )
    }

    private suspend fun deleteLocalGroupData(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ) {
        chatDao.hideGroupConversation(
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = groupId,
                createdAtEpochMilliseconds = deletedAtEpochMilliseconds
            )
        )
        groupSecurityManager.deleteLocalGroup(groupId).getOrThrow()
        groupVerificationDao.deleteByGroupId(groupId)
        groupInvitationDao.deleteByGroupId(groupId)
    }

    private suspend fun createRecipients(
        groupId: String,
        contacts: List<Contact>
    ): List<GroupWelcomeRecipient> =
        contacts.map { contact ->
            val invitation =
                groupInvitationDao.findByGroupAndContact(groupId, contact.id)
                    ?: error("Group invitation was not found for welcome recipient: ${contact.id}")
            GroupWelcomeRecipient(
                contactId = contact.id,
                invitationId = invitation.invitationId,
                encryptionPublicKey = requireNotNull(contact.secureChatIdentity).encryptionPublicKey.copyOf()
            )
        }

    private fun Contact.membershipDisplayName(): String =
        displayName?.trim()?.takeIf(String::isNotEmpty)
            ?: preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
            ?: "Member"

    private fun Contact.requirePhoneNumber(): String =
        preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
            ?: phoneNumbers
                .firstOrNull()
                ?.value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
            ?: error("Contact has no phone number: $id")

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val GROUP_OWNER_ROLE = "OWNER"
        const val GROUP_MEMBER_ROLE = "MEMBER"
        const val INITIAL_GROUP_EPOCH = 1
        const val INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
