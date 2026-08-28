package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.invitation.resolveInvitationUpdatedAt
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

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
    private val epochCoordinator: GroupEpochCoordinator
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
                requireContactsAreNotCurrentMembers(groupId, contactIds)

                val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val now = SystemClock.nowEpochMilliseconds()
                val newInvitations = mutableListOf<OutgoingInvitationDto>()

                loadContacts(contactIds).forEach { contact ->
                    val existing =
                        groupInvitationDao.findByGroupContactAndDirection(
                            groupId = groupId,
                            contactId = contact.id,
                            direction = GroupInvitationDirection.OUTGOING.name
                        )
                    when {
                        existing?.isUnacknowledgedOutgoingInvite() == true -> {
                            if (!resendUnacknowledgedInvite(existing)) {
                                groupInvitationDao.deleteByGroupContactAndDirection(
                                    groupId = groupId,
                                    contactId = contact.id,
                                    direction = GroupInvitationDirection.OUTGOING.name
                                )
                                newInvitations +=
                                    createOutgoingInvitation(
                                        groupId = groupId,
                                        title = requireNotNull(conversation.title),
                                        contact = contact,
                                        ownerIdentity = ownerIdentity,
                                        ownerSigningKeyPair = ownerSigningKeyPair,
                                        createdAt = now
                                    )
                            }
                        }

                        existing == null || existing.status.isTerminalStatus() -> {
                            groupInvitationDao.deleteByGroupContactAndDirection(
                                groupId = groupId,
                                contactId = contact.id,
                                direction = GroupInvitationDirection.OUTGOING.name
                            )
                            newInvitations +=
                                createOutgoingInvitation(
                                    groupId = groupId,
                                    title = requireNotNull(conversation.title),
                                    contact = contact,
                                    ownerIdentity = ownerIdentity,
                                    ownerSigningKeyPair = ownerSigningKeyPair,
                                    createdAt = now
                                )
                        }

                        else -> error("Contact already has an active group invitation")
                    }
                }

                persistInvitations(newInvitations)
                groupVerificationCoordinator.onOwnedMembershipChanged(groupId).getOrThrow()
                sendInvitations(newInvitations)
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

    private suspend fun requireContactsAreNotCurrentMembers(
        groupId: String,
        contactIds: Set<String>
    ) {
        val currentContactIds =
            epochCoordinator
                .findCurrentParticipants(groupId)
                .mapTo(mutableSetOf()) { participant -> participant.contactId }
        check(contactIds.none(currentContactIds::contains)) {
            "One or more contacts already belong to this group"
        }
    }

    private suspend fun resendUnacknowledgedInvite(invitation: GroupInvitationEntity): Boolean {
        val packetId = INVITE_PACKET_ID_PREFIX + invitation.invitationId
        val queuedPacket = protocolOutbox.findByPacketId(packetId).getOrThrow()
        if (queuedPacket == null) {
            markInvitationSendFailed(invitation)
            return false
        }
        protocolOutbox.resend(packetId).getOrThrow()
        return true
    }

    private fun GroupInvitationEntity.isUnacknowledgedOutgoingInvite(): Boolean =
        direction == GroupInvitationDirection.OUTGOING.name &&
            status == GroupInvitationStatus.INVITE_SENT.name

    private suspend fun loadContacts(contactIds: Set<String>): List<Contact> =
        contactIds.map { contactId -> identity.requireContact(contactId) }.sortedBy(Contact::id)

    private suspend fun createOutgoingInvitation(
        groupId: String,
        title: String,
        contact: Contact,
        ownerIdentity: LocalPublicIdentity,
        ownerSigningKeyPair: LocalSigningKeyPair,
        createdAt: Long
    ): OutgoingInvitationDto {
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
        return OutgoingInvitationDto(entity, packet)
    }

    private suspend fun persistInvitations(invitations: List<OutgoingInvitationDto>) {
        groupInvitationDao.upsertAll(invitations.map(OutgoingInvitationDto::entity))
    }

    private suspend fun sendInvitations(invitations: List<OutgoingInvitationDto>) {
        invitations.forEach { invitation ->
            protocolOutbox
                .enqueue(invitation.entity.contactId, invitation.packet)
                .onFailure { markInvitationSendFailed(invitation.entity) }
        }
    }

    suspend fun markInvitationTransportFailed(packetId: String) {
        if (!packetId.startsWith(INVITE_PACKET_ID_PREFIX)) return
        val invitationId = packetId.removePrefix(INVITE_PACKET_ID_PREFIX)
        if (invitationId.isBlank()) return
        val invitation = groupInvitationDao.findByInvitationId(invitationId) ?: return
        if (invitation.direction != GroupInvitationDirection.OUTGOING.name) return
        if (invitation.status != GroupInvitationStatus.INVITE_SENT.name) return
        markInvitationSendFailed(invitation)
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

    private data class OutgoingInvitationDto(
        val entity: GroupInvitationEntity,
        val packet: GroupInvitePacket
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val INVITE_PACKET_ID_PREFIX = "group-invite-"
        const val INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
