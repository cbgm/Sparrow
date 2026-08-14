package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.group.outgoing.GroupPacketBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.CreatedGroupSecurity
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_MEMBER_ROLE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.verification.GroupVerificationCoordinator
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

@Suppress("LongParameterList")
internal class GroupMembershipActivationCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupVerificationCoordinator: GroupVerificationCoordinator,
    private val membershipLock: GroupMembershipLock,
    private val identity: GroupMembershipIdentity,
    private val epochCoordinator: GroupEpochCoordinator,
    private val packetBroadcaster: GroupPacketBroadcaster
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
        val expectedSigningPublicKey =
            currentMemberKey(packet.groupId, memberContactId)
                ?.signingPublicKey
                ?: identity.requireContact(memberContactId).sparrowIdentity?.signingPublicKey
                ?: error("Group member signing identity was not found")
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
            .verifyReadyAcknowledgement(packet, expectedSigningPublicKey)
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
            check(currentMemberKey(groupId, memberContactId) != null) {
                "Ready acknowledgement came from a non-member"
            }
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
        val packetsByContactId = linkedMapOf<String, GroupMemberActivatedPacket>()

        epochCoordinator
            .loadCurrentParticipantContacts(packet.groupId)
            .filterNot { contact -> contact.id == memberContactId }
            .forEach { activeContact ->
                packetsByContactId[activeContact.id] =
                    createMemberActivation(
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

        packetsByContactId[memberContactId] =
            createMemberActivation(
                groupId = packet.groupId,
                epoch = packet.epoch,
                activationId = packet.packetId,
                activatedAtEpochMilliseconds = activationTimestamp,
                activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                memberContact = activatedContact,
                recipientContactId = memberContactId,
                ownerSigningKeyPair = adminSigningKeyPair
            )

        packetBroadcaster.enqueueAll(packetsByContactId).getOrThrow()
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

                val failures = mutableListOf<String>()
                readyInvitations.forEach { invitation ->
                    runCatching { distributeGroupKeyToMember(groupId, invitation) }
                        .onFailure { error ->
                            failures +=
                                "${invitation.contactId}: ${error.message ?: error::class.simpleName.orEmpty()}"
                        }
                }
                check(failures.isEmpty()) {
                    "Group key distribution failed for ${failures.joinToString()}"
                }
            }
        }

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String
    ): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                val participants = epochCoordinator.findCurrentParticipants(packet.groupId)
                val acknowledgingParticipant =
                    participants.firstOrNull { participant -> participant.contactId == acknowledgingContactId }
                        ?: error("Acknowledging group member was not found")
                val activatedParticipant =
                    participants.singleOrNull { participant ->
                        currentMemberKey(packet.groupId, participant.contactId)
                            ?.signingPublicKey
                            ?.contentEquals(packet.activatedMemberSigningPublicKey) == true
                    } ?: error("Activated group member was not found")
                val activatedContact = identity.requireContact(activatedParticipant.contactId)
                check(activatedContact.id != acknowledgingContactId) {
                    "A group member cannot acknowledge its own activation"
                }

                val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val acknowledgingContact = identity.requireContact(acknowledgingParticipant.contactId)
                val acknowledgedAt = packet.acknowledgedAtEpochMilliseconds

                when (packet.activationRound) {
                    GroupMemberActivatedPacket.DISCOVERY_ROUND ->
                        packetBroadcaster
                            .enqueueAll(
                                mapOf(
                                    activatedContact.id to
                                        createMemberActivation(
                                            groupId = packet.groupId,
                                            epoch = packet.epoch,
                                            activationId = packet.activationId,
                                            activatedAtEpochMilliseconds = acknowledgedAt,
                                            activationRound = GroupMemberActivatedPacket.RECIPROCAL_ROUND,
                                            memberContact = acknowledgingContact,
                                            recipientContactId = activatedContact.id,
                                            ownerSigningKeyPair = adminSigningKeyPair
                                        )
                                )
                            ).getOrThrow()

                    GroupMemberActivatedPacket.RECIPROCAL_ROUND -> {
                        val packetsByContactId =
                            linkedMapOf(
                                acknowledgingContactId to
                                    createMemberActivation(
                                        groupId = packet.groupId,
                                        epoch = packet.epoch,
                                        activationId = packet.activationId,
                                        activatedAtEpochMilliseconds = acknowledgedAt,
                                        activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                                        memberContact = activatedContact,
                                        recipientContactId = acknowledgingContactId,
                                        ownerSigningKeyPair = adminSigningKeyPair
                                    ),
                                activatedContact.id to
                                    createMemberActivation(
                                        groupId = packet.groupId,
                                        epoch = packet.epoch,
                                        activationId = packet.activationId,
                                        activatedAtEpochMilliseconds = acknowledgedAt,
                                        activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                                        memberContact = acknowledgingContact,
                                        recipientContactId = activatedContact.id,
                                        ownerSigningKeyPair = adminSigningKeyPair
                                    )
                            )
                        packetBroadcaster.enqueueAll(packetsByContactId).getOrThrow()
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
        val isExistingParticipant = currentMemberKey(groupId, contactId) != null
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
        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
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

    private suspend fun createMemberActivation(
        groupId: String,
        epoch: Int,
        activationId: String,
        activatedAtEpochMilliseconds: Long,
        activationRound: Int,
        memberContact: Contact,
        recipientContactId: String,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): GroupMemberActivatedPacket {
        val memberKey =
            currentMemberKey(groupId, memberContact.id)
                ?: error("Activated group member is not part of the current group epoch")
        return membershipPacketProtocol
            .createMemberActivated(
                groupId = groupId,
                epoch = epoch,
                member =
                    GroupMemberPayload(
                        displayName = memberContact.displayName,
                        encryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
                        signingPublicKey = memberKey.signingPublicKey.copyOf(),
                        role = memberKey.role,
                        phoneNumber = memberContact.requireGroupPhoneNumber()
                    ),
                activatedAtEpochMilliseconds = activatedAtEpochMilliseconds,
                activationRound = activationRound,
                activationId = activationId,
                memberReferenceId = memberContact.id,
                recipientContactId = recipientContactId,
                ownerSigningKeyPair = ownerSigningKeyPair
            ).getOrThrow()
    }

    private suspend fun currentMemberKey(
        groupId: String,
        contactId: String
    ): GroupMemberKeyEntity? =
        groupSecurityManager
            .findRemoteMemberKey(
                groupId = groupId,
                contactId = contactId
            ).getOrThrow()

    private companion object {
        const val INITIAL_GROUP_EPOCH = 1
    }
}
