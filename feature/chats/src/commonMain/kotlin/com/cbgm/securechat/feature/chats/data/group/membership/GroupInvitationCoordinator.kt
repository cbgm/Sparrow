package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.contacts.domain.model.Contact

@Suppress("LongParameterList")
internal class GroupInvitationCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator,
    private val activation: GroupMembershipActivationCoordinator,
    private val administration: GroupMembershipAdministrationCoordinator
) {
    suspend fun createGroup(
        title: String,
        contactIds: Set<String>
    ): Result<String> =
        runCatching {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Group title must not be blank" }
            require(contactIds.isNotEmpty()) { "A group requires at least one contact" }

            val contacts = loadContacts(contactIds)
            val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val groupId = IdGenerator.generate(prefix = "group")

            chatDao.upsertConversation(
                ConversationEntity(
                    id = groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = normalizedTitle,
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )
            )

            val invitations =
                contacts.map { contact ->
                    createOutgoingInvitation(
                        groupId = groupId,
                        title = normalizedTitle,
                        contact = contact,
                        ownerIdentity = ownerIdentity,
                        ownerSigningKeyPair = ownerSigningKeyPair,
                        createdAt = now
                    )
                }
            persistInvitations(invitations)
            groupVerificationCoordinator.initializeOwnedGroup(groupId).getOrThrow()
            sendInvitations(invitations)
            groupId
        }

    suspend fun addMembers(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(contactIds.isNotEmpty()) { "Choose at least one contact" }

            membershipLock.withLock {
                val conversation = requireOwnedGroupConversation(groupId)
                requireContactsCanBeAdded(groupId, contactIds)

                val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val now = SystemClock.nowEpochMilliseconds()
                val invitations =
                    loadContacts(contactIds).map { contact ->
                        groupInvitationDao.deleteByGroupAndContact(groupId, contact.id)
                        createOutgoingInvitation(
                            groupId = groupId,
                            title = requireNotNull(conversation.title),
                            contact = contact,
                            ownerIdentity = ownerIdentity,
                            ownerSigningKeyPair = ownerSigningKeyPair,
                            createdAt = now
                        )
                    }

                persistInvitations(invitations)
                groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
                sendInvitations(invitations)
                chatDao.updateConversationTimestamp(groupId, now)
            }
        }

    private suspend fun requireOwnedGroupConversation(groupId: String): ConversationEntity {
        val conversation =
            chatDao.findConversationById(groupId)
                ?: error("Group conversation was not found")
        check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }
        groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        return conversation
    }

    private suspend fun requireContactsCanBeAdded(
        groupId: String,
        contactIds: Set<String>
    ) {
        val unavailableContactIds = currentOrPendingContactIds(groupId)
        check(contactIds.none(unavailableContactIds::contains)) {
            "One or more contacts already belong to this group"
        }
    }

    private suspend fun currentOrPendingContactIds(groupId: String): Set<String> {
        val result =
            epochCoordinator
                .findCurrentParticipants(groupId)
                .mapTo(mutableSetOf()) { participant -> participant.contactId }
        groupInvitationDao
            .findByGroupId(groupId)
            .filter { invitation ->
                !invitation.status.isTerminalStatus() &&
                    invitation.status != GroupInvitationStatus.ACTIVE.name
            }.mapTo(result, GroupInvitationEntity::contactId)
        return result
    }

    private suspend fun loadContacts(contactIds: Set<String>): List<Contact> =
        contactIds.map { contactId -> identity.requireContact(contactId) }.sortedBy(Contact::id)

    private suspend fun createOutgoingInvitation(
        groupId: String,
        title: String,
        contact: Contact,
        ownerIdentity: LocalPublicIdentity,
        ownerSigningKeyPair: LocalSigningKeyPair,
        createdAt: Long
    ): OutgoingInvitation {
        val invitationId = IdGenerator.generate(prefix = "group-invitation")
        val expiresAt = createdAt + INVITATION_VALIDITY_MILLISECONDS
        val packet =
            membershipPacketProtocol
                .createInvite(
                    invitationId = invitationId,
                    groupId = groupId,
                    title = title,
                    createdAtEpochMilliseconds = createdAt,
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
                createdAtEpochMilliseconds = createdAt,
                expiresAtEpochMilliseconds = expiresAt,
                updatedAtEpochMilliseconds = createdAt
            )
        return OutgoingInvitation(entity, packet)
    }

    private suspend fun persistInvitations(invitations: List<OutgoingInvitation>) {
        groupInvitationDao.upsertAll(invitations.map(OutgoingInvitation::entity))
    }

    private suspend fun sendInvitations(invitations: List<OutgoingInvitation>) {
        invitations.forEach { invitation ->
            protocolOutbox
                .enqueue(invitation.entity.contactId, invitation.packet)
                .onFailure { markInvitationSendFailed(invitation.entity) }
        }
    }

    private suspend fun markInvitationSendFailed(invitation: GroupInvitationEntity) {
        groupInvitationDao.updateStatus(
            invitationId = invitation.invitationId,
            expectedStatus = GroupInvitationStatus.INVITE_SENT.name,
            newStatus =
                GroupMembershipStateMachine
                    .transition(
                        GroupInvitationStatus.INVITE_SENT.name,
                        GroupMembershipEvent.INVITE_SEND_FAILED
                    ).name,
            updatedAt = maxOf(invitation.createdAtEpochMilliseconds, SystemClock.nowEpochMilliseconds())
        )
    }

    suspend fun receiveInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            membershipPacketProtocol.verifyInvite(packet).getOrThrow()
            if (shouldIgnoreIncomingInvite(packet)) return@runCatching
            if (isExistingIncomingInvite(ownerContactId, packet)) return@runCatching

            groupSecurityManager.clearRetiredMembershipBeforeRejoin(packet.groupId).getOrThrow()
            val replacedInvitation = findReplaceableInvitation(ownerContactId, packet)
            val persistedAt =
                resolveInvitationUpdatedAt(
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    candidateAtEpochMilliseconds = receivedAtEpochMilliseconds
                )
            updateIncomingOwnerIdentity(ownerContactId, packet, persistedAt)
            storeIncomingInvite(ownerContactId, packet, persistedAt, replacedInvitation != null)
        }

    private suspend fun shouldIgnoreIncomingInvite(packet: GroupInvitePacket): Boolean {
        val localDeletionTimestamp =
            chatDao.findMessageTimestampByTransportMode(
                conversationId = packet.groupId,
                transportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE
            ) ?: return false

        if (packet.createdAtEpochMilliseconds <= localDeletionTimestamp) return true
        chatDao.deleteConversationMessages(packet.groupId)
        return false
    }

    private suspend fun isExistingIncomingInvite(
        ownerContactId: String,
        packet: GroupInvitePacket
    ): Boolean {
        val existing = groupInvitationDao.findByInvitationId(packet.invitationId) ?: return false
        check(
            existing.groupId == packet.groupId &&
                existing.contactId == ownerContactId &&
                existing.challenge.contentEquals(packet.challenge)
        ) {
            "Group invitation conflicts with an existing invitation"
        }
        return true
    }

    private suspend fun findReplaceableInvitation(
        ownerContactId: String,
        packet: GroupInvitePacket
    ): GroupInvitationEntity? {
        val existing = groupInvitationDao.findByGroupAndContact(packet.groupId, ownerContactId)
        if (existing != null) {
            check(existing.status.isTerminalStatus()) {
                "A current group invitation already exists for this group"
            }
        }
        return existing
    }

    private suspend fun updateIncomingOwnerIdentity(
        ownerContactId: String,
        packet: GroupInvitePacket,
        persistedAt: Long
    ) {
        val identityChanged =
            identity.stageIncomingOwnerIdentity(
                contactId = ownerContactId,
                encryptionPublicKey = packet.ownerEncryptionPublicKey,
                signingPublicKey = packet.ownerSigningPublicKey
            )
        if (!identityChanged) return

        groupInvitationDao.failSupersededIncomingInvitations(
            contactId = ownerContactId,
            currentInvitationId = packet.invitationId,
            awaitingAcceptanceStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
            failedStatus = GroupInvitationStatus.FAILED.name,
            updatedAt = persistedAt
        )
    }

    private suspend fun storeIncomingInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        persistedAt: Long,
        replacesExisting: Boolean
    ) {
        chatDao.upsertConversation(
            ConversationEntity(
                id = packet.groupId,
                contactId = null,
                type = GROUP_CONVERSATION_TYPE,
                title = packet.title,
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
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
                ownerEncryptionPublicKey = packet.ownerEncryptionPublicKey.copyOf(),
                ownerSigningPublicKey = packet.ownerSigningPublicKey.copyOf(),
                createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                updatedAtEpochMilliseconds = persistedAt
            )
        if (replacesExisting) {
            groupInvitationDao.replaceForGroupAndContact(invitation)
        } else {
            groupInvitationDao.upsert(invitation)
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
                    newStatus = GroupMembershipStateMachine.transition(invitation.status, GroupMembershipEvent.EXPIRE).name,
                    updatedAt =
                        resolveInvitationUpdatedAt(
                            createdAtEpochMilliseconds = invitation.createdAtEpochMilliseconds,
                            candidateAtEpochMilliseconds = now
                        )
                )
                error("Group invitation has expired")
            }

            identity.requireAcceptedOwnerIdentity(invitation)
            val memberIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val joinRequest =
                membershipPacketProtocol
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
                    newStatus = GroupMembershipStateMachine.transition(invitation.status, GroupMembershipEvent.ACCEPT).name,
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
                    newStatus = GroupMembershipStateMachine.transition(GroupInvitationStatus.JOIN_SENT.name, GroupMembershipEvent.JOIN_SEND_FAILED).name,
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
                membershipPacketProtocol
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
                    newStatus = GroupMembershipStateMachine.transition(invitation.status, GroupMembershipEvent.DECLINE).name,
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

            membershipPacketProtocol.verifyJoinRequest(packet).getOrThrow()

            if (
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                invitation.status == GroupInvitationStatus.ACTIVE.name
            ) {
                return@runCatching
            }

            identity.storeMutualIdentity(
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
                        newStatus = GroupMembershipStateMachine.transition(invitation.status, GroupMembershipEvent.IDENTITY_CONFIRMED).name,
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

            activation.activateGroupIfReady(packet.groupId).getOrThrow()
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
            membershipPacketProtocol.verifyDecline(packet).getOrThrow()
            identity.ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)

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
                administration
                    .removeDepartingMember(
                        groupId = packet.groupId,
                        contactId = memberContactId
                    ).getOrThrow()
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
                    newStatus = GroupMembershipStateMachine.transition(invitation.status, GroupMembershipEvent.DECLINE).name,
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

    private suspend fun requireIncomingInvitation(groupId: String): GroupInvitationEntity {
        val invitations = groupInvitationDao.findByGroupId(groupId)
        return invitations
            .singleOrNull { invitation ->
                invitation.direction == GroupInvitationDirection.INCOMING.name
            } ?: error("Incoming group invitation was not found")
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

    private fun String.isTerminalStatus(): Boolean =
        this == GroupInvitationStatus.DECLINED.name ||
            this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.EXPIRED.name ||
            this == GroupInvitationStatus.FAILED.name

    private data class OutgoingInvitation(
        val entity: GroupInvitationEntity,
        val packet: GroupInvitePacket
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
