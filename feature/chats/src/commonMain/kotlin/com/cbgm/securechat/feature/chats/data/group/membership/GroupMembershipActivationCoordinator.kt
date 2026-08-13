package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.securechat.feature.chats.data.group.security.CreatedGroupSecurity
import com.cbgm.securechat.feature.chats.data.group.security.GROUP_MEMBER_ROLE
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.securechat.feature.contacts.domain.model.Contact

@Suppress("LongParameterList")
internal class GroupMembershipActivationCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator
) {
    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                val invitation = groupInvitationDao.findByGroupAndContact(packet.groupId, memberContactId)
                validateReadyAcknowledgement(memberContactId, packet, invitation)
                if (!shouldActivateReadyMember(memberContactId, packet.groupId, invitation)) {
                    return@withLock
                }
                activateReadyMember(
                    memberContactId = memberContactId,
                    packet = packet,
                    invitation = requireNotNull(invitation),
                    receivedAt = receivedAtEpochMilliseconds
                )
            }
        }

    private suspend fun validateReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        invitation: GroupInvitationEntity?
    ) {
        val referenceId = invitation?.invitationId ?: "member-$memberContactId"
        val expectedIdentity =
            identity.requireContact(memberContactId).secureChatIdentity
                ?: error("Group member identity was not found")
        val expectedWelcomePacketId =
            groupSecurityManager.welcomePacketId(
                groupId = packet.groupId,
                invitationId = referenceId,
                epoch = packet.epoch
            )
        check(packet.welcomePacketId == expectedWelcomePacketId) {
            "Ready acknowledgement references the wrong welcome"
        }
        membershipPacketProtocol
            .verifyReadyAcknowledgement(packet, expectedIdentity.signingPublicKey)
            .getOrThrow()
        groupSecurityManager
            .verifyKeyConfirmation(
                groupId = packet.groupId,
                epoch = packet.epoch,
                keyConfirmation = packet.keyConfirmation
            ).getOrThrow()
    }

    private suspend fun shouldActivateReadyMember(
        memberContactId: String,
        groupId: String,
        invitation: GroupInvitationEntity?
    ): Boolean {
        if (invitation == null) {
            check(
                chatDao.findConversationParticipants(groupId)
                    .any { participant -> participant.contactId == memberContactId }
            ) { "Ready acknowledgement came from a non-member" }
            return false
        }
        if (invitation.status == GroupInvitationStatus.ACTIVE.name) return false
        check(invitation.status == GroupInvitationStatus.WELCOME_SENT.name) {
            "Group member is not waiting for a ready acknowledgement"
        }
        return true
    }

    private suspend fun activateReadyMember(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        invitation: GroupInvitationEntity,
        receivedAt: Long
    ) {
        val activatedContact = identity.requireContact(memberContactId)
        val activationTimestamp = maxOf(invitation.createdAtEpochMilliseconds, receivedAt)
        sendActivationPackets(
            memberContactId = memberContactId,
            packet = packet,
            activatedContact = activatedContact,
            activationTimestamp = activationTimestamp
        )
        markMemberActive(invitation, activationTimestamp)
        persistActiveParticipant(packet.groupId, memberContactId, activationTimestamp)
        recordMemberAddedIfNeeded(packet, invitation, activatedContact, activationTimestamp)
        groupVerificationCoordinator.onOwnedMembershipChanged(packet.groupId).getOrThrow()
    }

    private suspend fun sendActivationPackets(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        activatedContact: Contact,
        activationTimestamp: Long
    ) {
        val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        epochCoordinator.loadCurrentParticipantContacts(packet.groupId)
            .filterNot { contact -> contact.id == memberContactId }
            .forEach { activeContact ->
                enqueueMemberActivation(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    activationId = packet.packetId,
                    activatedAtEpochMilliseconds = activationTimestamp,
                    activationRound = GroupMemberActivatedPacket.DISCOVERY_ROUND,
                    memberContact = activatedContact,
                    recipientContactId = activeContact.id,
                    ownerSigningKeyPair = adminSigningKeyPair
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
            ownerSigningKeyPair = adminSigningKeyPair
        )
    }

    private suspend fun markMemberActive(
        invitation: GroupInvitationEntity,
        activationTimestamp: Long
    ) {
        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = GroupInvitationStatus.WELCOME_SENT.name,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        invitation.status,
                        GroupMembershipEvent.MEMBER_READY
                    ).name,
                updatedAt = activationTimestamp
            )
        check(updated == 1) { "Group invitation changed while readiness was applied" }
    }

    private suspend fun persistActiveParticipant(
        groupId: String,
        memberContactId: String,
        joinedAt: Long
    ) {
        chatDao.upsertConversationParticipant(
            ConversationParticipantEntity(
                conversationId = groupId,
                contactId = memberContactId,
                role = GROUP_MEMBER_ROLE,
                joinedAtEpochMilliseconds = joinedAt
            )
        )
    }

    private suspend fun recordMemberAddedIfNeeded(
        packet: GroupReadyAcknowledgementPacket,
        invitation: GroupInvitationEntity,
        contact: Contact,
        createdAt: Long
    ) {
        if (!chatDao.hasMessages(packet.groupId)) return
        chatDao.upsertMessage(
            GroupMembershipMessageFactory.memberAdded(
                conversationId = packet.groupId,
                epoch = packet.epoch,
                contactId = contact.id,
                contactName = contact.groupMembershipDisplayName(),
                createdAtEpochMilliseconds = createdAt,
                eventId = invitation.invitationId
            )
        )
        chatDao.updateConversationTimestamp(packet.groupId, createdAt)
    }

    suspend fun activateGroupIfReady(groupId: String): Result<Unit> =
        runCatching {
            membershipLock.withLock {
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
            membershipLock.withLock {
                val participants = chatDao.findConversationParticipants(packet.groupId)
                val acknowledgingParticipant =
                    participants.firstOrNull { participant -> participant.contactId == acknowledgingContactId }
                        ?: error("Acknowledging group member was not found")
                val activatedContact =
                    participants
                        .map { participant -> identity.requireContact(participant.contactId) }
                        .singleOrNull { contact ->
                            contact.secureChatIdentity
                                ?.signingPublicKey
                                ?.contentEquals(packet.activatedMemberSigningPublicKey) == true
                        } ?: error("Activated group member was not found")
                check(activatedContact.id != acknowledgingContactId) {
                    "A group member cannot acknowledge its own activation"
                }

                val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val acknowledgingContact = identity.requireContact(acknowledgingParticipant.contactId)
                val acknowledgedAt = packet.acknowledgedAtEpochMilliseconds

                when (packet.activationRound) {
                    GroupMemberActivatedPacket.DISCOVERY_ROUND ->
                        enqueueMemberActivation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            activatedAtEpochMilliseconds = acknowledgedAt,
                            activationRound = GroupMemberActivatedPacket.RECIPROCAL_ROUND,
                            memberContact = acknowledgingContact,
                            recipientContactId = activatedContact.id,
                            ownerSigningKeyPair = adminSigningKeyPair
                        )

                    GroupMemberActivatedPacket.RECIPROCAL_ROUND -> {
                        enqueueMemberActivation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            activatedAtEpochMilliseconds = acknowledgedAt,
                            activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                            memberContact = activatedContact,
                            recipientContactId = acknowledgingContactId,
                            ownerSigningKeyPair = adminSigningKeyPair
                        )
                        enqueueMemberActivation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            activationId = packet.activationId,
                            activatedAtEpochMilliseconds = acknowledgedAt,
                            activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                            memberContact = acknowledgingContact,
                            recipientContactId = activatedContact.id,
                            ownerSigningKeyPair = adminSigningKeyPair
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
        val conversation =
            chatDao.findConversationById(groupId)
                ?: error("Pending group was not found")
        val contact = identity.requireContact(invitation.contactId)
        check(contact.hasMutualGroupIdentity()) {
            "Group member identity is not ready: ${contact.id}"
        }

        val members = loadGroupKeyDistributionMembers(groupId, contact)
        val roleOverrides = memberRoleOverrides(groupId, contact.id)
        val securedGroup =
            createWelcomePackets(
                groupId = groupId,
                conversation = conversation,
                members = members,
                roleOverrides = roleOverrides
            )

        check(contact.id in securedGroup.welcomePacketsByContactId) {
            "Recipient welcome packet was not created"
        }
        enqueueWelcomePackets(securedGroup)
        markWelcomeSent(invitation)
    }

    private suspend fun loadGroupKeyDistributionMembers(
        groupId: String,
        newMember: Contact
    ): List<Contact> =
        (epochCoordinator.loadCurrentParticipantContacts(groupId) + newMember)
            .distinctBy(Contact::id)
            .sortedBy(Contact::id)

    private suspend fun memberRoleOverrides(
        groupId: String,
        contactId: String
    ): Map<String, String> {
        val isExistingParticipant =
            chatDao.findConversationParticipants(groupId)
                .any { participant -> participant.contactId == contactId }
        return if (isExistingParticipant) {
            mapOf(contactId to GROUP_MEMBER_ROLE)
        } else {
            emptyMap()
        }
    }

    private suspend fun createWelcomePackets(
        groupId: String,
        conversation: ConversationEntity,
        members: List<Contact>,
        roleOverrides: Map<String, String>
    ): CreatedGroupSecurity {
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val currentEpoch = groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        val targetEpoch = currentEpoch?.plus(1) ?: INITIAL_GROUP_EPOCH
        val memberPayloads =
            epochCoordinator.createMemberPayloads(
                groupId = groupId,
                localIdentity = localIdentity,
                localPhoneNumber = localPhoneNumber,
                contacts = members,
                roleOverrides = roleOverrides
            )
        val memberKeys = epochCoordinator.createMemberKeys(groupId, targetEpoch, members, roleOverrides)
        val recipients = epochCoordinator.createRecipients(groupId, members)
        val title = requireNotNull(conversation.title)

        return if (currentEpoch == null) {
            groupSecurityManager
                .createOwnedGroup(
                    groupId = groupId,
                    title = title,
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    memberPayloads = memberPayloads,
                    memberKeys = memberKeys,
                    recipients = recipients,
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()
        } else {
            groupSecurityManager
                .rotateOwnedGroup(
                    groupId = groupId,
                    title = title,
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                    memberPayloads = memberPayloads,
                    memberKeys = memberKeys,
                    recipients = recipients,
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()
        }
    }

    private suspend fun enqueueWelcomePackets(securedGroup: CreatedGroupSecurity) {
        securedGroup.welcomePacketsByContactId.forEach { (contactId, packet) ->
            protocolOutbox.enqueue(contactId, packet).getOrThrow()
        }
    }

    private suspend fun markWelcomeSent(invitation: GroupInvitationEntity) {
        val updated =
            groupInvitationDao.updateStatus(
                invitationId = invitation.invitationId,
                expectedStatus = GroupInvitationStatus.IDENTITY_READY.name,
                newStatus =
                    GroupMembershipStateMachine
                        .transition(invitation.status, GroupMembershipEvent.WELCOME_SENT)
                        .name,
                updatedAt =
                    maxOf(
                        invitation.createdAtEpochMilliseconds,
                        SystemClock.nowEpochMilliseconds()
                    )
            )
        check(updated == 1) {
            "Group invitation changed while its welcome was recorded"
        }
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
        val memberRole =
            groupSecurityManager
                .findRemoteMemberKey(groupId, memberContact.id)
                .getOrThrow()
                ?.role
                ?: GROUP_MEMBER_ROLE
        val packet =
            membershipPacketProtocol
                .createMemberActivated(
                    groupId = groupId,
                    epoch = epoch,
                    member =
                        GroupMemberPayload(
                            displayName = memberContact.displayName,
                            encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                            signingPublicKey = identity.signingPublicKey.copyOf(),
                            role = memberRole,
                            phoneNumber = memberContact.requireGroupPhoneNumber()
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

    private companion object {
        const val INITIAL_GROUP_EPOCH = 1
    }
}
