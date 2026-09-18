package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipBroadcastDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipConversationDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipMessageDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipPeerDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipProtocolDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipSecurityDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipStoreDataSource
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipVerificationDataSource
import com.cbgm.sparrow.feature.membership.data.groupMembershipDisplayName
import com.cbgm.sparrow.feature.membership.data.hasMutualGroupIdentity
import com.cbgm.sparrow.feature.membership.data.model.CreatedGroupSecurityDto
import com.cbgm.sparrow.feature.membership.data.model.GROUP_MEMBER_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.requireGroupPhoneNumber

@Suppress("LongParameterList")
class GroupMembershipActivationDataSource(
    private val conversationDataSource: GroupMembershipConversationDataSource,
    private val membershipStore: GroupMembershipStoreDataSource,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val membershipPacketProtocol: GroupMembershipProtocolDataSource,
    private val groupSecurityManager: GroupMembershipSecurityDataSource,
    private val verificationDataSource: GroupMembershipVerificationDataSource,
    private val membershipLock: GroupMembershipLock,
    private val peerDataSource: GroupMembershipPeerDataSource,
    private val epochDataSource: GroupEpochDataSource,
    private val packetBroadcaster: GroupMembershipBroadcastDataSource,
    private val membershipMessageDataSource: GroupMembershipMessageDataSource
) {
    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                val membership = membershipStore.findByGroupAndContact(packet.groupId, memberContactId)
                validateReadyAcknowledgement(memberContactId, packet, membership)
                if (!shouldActivateReadyMember(memberContactId, packet.groupId, membership)) {
                    return@withLock
                }
                activateReadyMember(
                    memberContactId = memberContactId,
                    packet = packet,
                    membership = requireNotNull(membership),
                    receivedAt = receivedAtEpochMilliseconds
                )
            }
        }

    private suspend fun validateReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        membership: GroupMembershipEntity?
    ) {
        val referenceId = membership?.sourceInvitationId ?: "member-$memberContactId"
        val expectedSigningPublicKey =
            currentMemberKey(packet.groupId, memberContactId)
                ?.signingPublicKey
                ?: peerDataSource.requirePeer(memberContactId).signingPublicKey
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
        membership: GroupMembershipEntity?
    ): Boolean {
        if (membership == null) {
            check(currentMemberKey(groupId, memberContactId) != null) {
                "Ready acknowledgement came from a non-member"
            }
            return false
        }
        if (membership.status == GroupMembershipStatus.ACTIVE.name) return false
        check(membership.status == GroupMembershipStatus.WELCOME_SENT.name) {
            "Group member is not waiting for a ready acknowledgement"
        }
        return true
    }

    private suspend fun activateReadyMember(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        membership: GroupMembershipEntity,
        receivedAt: Long
    ) {
        val activatedContact = peerDataSource.requirePeer(memberContactId)
        val activationTimestamp = maxOf(membership.createdAtEpochMilliseconds, receivedAt)
        sendActivationPackets(
            memberContactId = memberContactId,
            packet = packet,
            activatedContact = activatedContact,
            activationTimestamp = activationTimestamp
        )
        markMemberActive(membership, activationTimestamp)
        persistActiveParticipant(packet.groupId, memberContactId, activationTimestamp)
        recordMemberAddedIfNeeded(packet, membership, activatedContact, activationTimestamp)
        verificationDataSource.onOwnedMembershipChanged(packet.groupId).getOrThrow()
    }

    private suspend fun sendActivationPackets(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        activatedContact: GroupMembershipPeerDto,
        activationTimestamp: Long
    ) {
        val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val packetsByContactId = linkedMapOf<String, GroupMemberActivatedPacket>()

        epochDataSource
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
        membership: GroupMembershipEntity,
        activationTimestamp: Long
    ) {
        val updated =
            membershipStore.updateStatus(
                membershipId = membership.membershipId,
                expectedStatus = GroupMembershipStatus.WELCOME_SENT.name,
                newStatus =
                    GroupMembershipStateMachine.transition(
                        membership.status,
                        GroupMembershipEvent.MEMBER_READY
                    ).name,
                updatedAt = activationTimestamp
            )
        check(updated == 1) { "Group membership changed while readiness was applied" }
    }

    private suspend fun persistActiveParticipant(
        groupId: String,
        memberContactId: String,
        joinedAt: Long
    ) {
        conversationDataSource.upsertConversationParticipant(
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
        membership: GroupMembershipEntity,
        contact: GroupMembershipPeerDto,
        createdAt: Long
    ) {
        conversationDataSource.upsertMessage(
            membershipMessageDataSource.memberAdded(
                conversationId = packet.groupId,
                epoch = packet.epoch,
                contactId = contact.id,
                contactName = contact.groupMembershipDisplayName(),
                createdAtEpochMilliseconds = createdAt,
                eventId = membership.sourceInvitationId
            )
        )
        conversationDataSource.updateConversationTimestamp(packet.groupId, createdAt)
    }

    suspend fun activateGroupIfReady(groupId: String): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                val readyMemberships =
                    membershipStore
                        .findByGroupId(groupId)
                        .filter { membership -> membership.status == GroupMembershipStatus.IDENTITY_READY.name }
                        .sortedBy(GroupMembershipEntity::sourceInvitationId)

                val failures = mutableListOf<String>()
                readyMemberships.forEach { membership ->
                    runCatching { distributeGroupKeyToMember(groupId, membership) }
                        .onFailure { error ->
                            failures +=
                                "${membership.contactId}: ${error.message ?: error::class.simpleName.orEmpty()}"
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
                val participants = epochDataSource.findCurrentParticipants(packet.groupId)
                val acknowledgingParticipant =
                    participants.firstOrNull { participant -> participant.contactId == acknowledgingContactId }
                        ?: error("Acknowledging group member was not found")
                val activatedParticipant =
                    participants.singleOrNull { participant ->
                        currentMemberKey(packet.groupId, participant.contactId)
                            ?.signingPublicKey
                            ?.contentEquals(packet.activatedMemberSigningPublicKey) == true
                    } ?: error("Activated group member was not found")
                val activatedContact = peerDataSource.requirePeer(activatedParticipant.contactId)
                check(activatedContact.id != acknowledgingContactId) {
                    "A group member cannot acknowledge its own activation"
                }

                val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val acknowledgingContact = peerDataSource.requirePeer(acknowledgingParticipant.contactId)
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
        membership: GroupMembershipEntity
    ) {
        val conversation =
            conversationDataSource.findConversationById(groupId)
                ?: error("Pending group was not found")
        val contact = peerDataSource.requirePeer(membership.contactId)
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
        markWelcomeSent(membership)
    }

    private suspend fun loadGroupKeyDistributionMembers(
        groupId: String,
        newMember: GroupMembershipPeerDto
    ): List<GroupMembershipPeerDto> =
        (epochDataSource.loadCurrentParticipantContacts(groupId) + newMember)
            .distinctBy(GroupMembershipPeerDto::id)
            .sortedBy(GroupMembershipPeerDto::id)

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
        members: List<GroupMembershipPeerDto>,
        roleOverrides: Map<String, String>
    ): CreatedGroupSecurityDto {
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val currentEpoch = groupSecurityManager.findOwnedGroupEpoch(groupId).getOrThrow()
        val targetEpoch = currentEpoch?.plus(1) ?: INITIAL_GROUP_EPOCH
        val memberPayloads =
            epochDataSource.createMemberPayloads(
                groupId = groupId,
                localIdentity = localIdentity,
                localPhoneNumber = localPhoneNumber,
                contacts = members,
                roleOverrides = roleOverrides
            )
        val memberKeys = epochDataSource.createMemberKeys(groupId, targetEpoch, members, roleOverrides)
        val recipients = epochDataSource.createRecipients(groupId, members)
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

    private suspend fun enqueueWelcomePackets(securedGroup: CreatedGroupSecurityDto) {
        packetBroadcaster.enqueueAll(securedGroup.welcomePacketsByContactId).getOrThrow()
    }

    private suspend fun markWelcomeSent(membership: GroupMembershipEntity) {
        val updated =
            membershipStore.updateStatus(
                membershipId = membership.membershipId,
                expectedStatus = GroupMembershipStatus.IDENTITY_READY.name,
                newStatus =
                    GroupMembershipStateMachine
                        .transition(membership.status, GroupMembershipEvent.WELCOME_SENT)
                        .name,
                updatedAt =
                    maxOf(
                        membership.createdAtEpochMilliseconds,
                        SystemClock.nowEpochMilliseconds()
                    )
            )
        check(updated == 1) {
            "Group membership changed while its welcome was recorded"
        }
    }

    private suspend fun createMemberActivation(
        groupId: String,
        epoch: Int,
        activationId: String,
        activatedAtEpochMilliseconds: Long,
        activationRound: Int,
        memberContact: GroupMembershipPeerDto,
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
