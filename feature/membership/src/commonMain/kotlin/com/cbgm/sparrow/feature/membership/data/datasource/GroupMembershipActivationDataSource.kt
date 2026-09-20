package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol

@Suppress("LongParameterList")
internal class GroupMembershipActivationDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val membershipLock: GroupMembershipLock,
    private val securityStore: GroupSecurityStoreDataSource,
    private val packetBroadcaster: GroupPacketBroadcaster
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

                val activationTimestamp =
                    maxOf(requireNotNull(membership).createdAtEpochMilliseconds, receivedAtEpochMilliseconds)
                sendActivationPackets(
                    memberContactId = memberContactId,
                    packet = packet,
                    activationTimestamp = activationTimestamp
                )
                markMemberActive(membership, activationTimestamp)
            }
        }

    private suspend fun validateReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        membership: GroupMembershipEntity?
    ) {
        val referenceId = membership?.sourceInvitationId ?: "member-$memberContactId"
        val memberKey =
            securityStore.findMemberKey(
                groupId = packet.groupId,
                epoch = packet.epoch,
                contactId = memberContactId
            ) ?: error("Group member signing identity was not found")
        val expectedWelcomePacketId =
            membershipPacketProtocol.welcomePacketId(
                groupId = packet.groupId,
                invitationId = referenceId,
                epoch = packet.epoch
            )
        check(packet.welcomePacketId == expectedWelcomePacketId) {
            "Ready acknowledgement references the wrong welcome"
        }
        membershipPacketProtocol
            .verifyReadyAcknowledgement(packet, memberKey.signingPublicKey)
            .getOrThrow()
        val state = securityStore.findState(packet.groupId)
            ?: error("Group security state was not found")
        check(state.currentEpoch == packet.epoch) {
            "Ready acknowledgement uses a non-current group epoch"
        }
    }

    private suspend fun shouldActivateReadyMember(
        memberContactId: String,
        groupId: String,
        membership: GroupMembershipEntity?
    ): Boolean {
        if (membership == null) {
            val state = securityStore.findState(groupId) ?: error("Group security state was not found")
            check(securityStore.findMemberKey(groupId, state.currentEpoch, memberContactId) != null) {
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

    private suspend fun sendActivationPackets(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        activationTimestamp: Long
    ) {
        val memberKeys = securityStore.findMemberKeys(packet.groupId, packet.epoch)
        val activatedMember =
            memberKeys.singleOrNull { memberKey -> memberKey.contactId == memberContactId }
                ?: error("Activated group member is not part of the current group epoch")
        val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val packetsByContactId = linkedMapOf<String, GroupMemberActivatedPacket>()
        val previousEpochKeys = if (packet.epoch > 1) {
            securityStore.findMemberKeys(packet.groupId, packet.epoch - 1)
                .associateBy { it.contactId }
        } else {
            emptyMap()
        }

        memberKeys
            .filterNot { memberKey -> memberKey.contactId == memberContactId }
            .forEach { activeMember ->
                // A current-epoch key may belong to a still-pending invitation.
                // Prefer the authoritative ACTIVE handshake where we own one.
                // Existing peers who joined through another admin may have no
                // owner-side handshake on this device; their key must already
                // have existed unchanged in the PREVIOUS epoch to be notified.
                val existingMembership = membershipStore.findByGroupAndContact(
                    packet.groupId,
                    activeMember.contactId
                )
                if (existingMembership != null) {
                    if (existingMembership.status != GroupMembershipStatus.ACTIVE.name) {
                        return@forEach
                    }
                } else {
                    val previousKey = previousEpochKeys[activeMember.contactId]
                        ?: return@forEach
                    if (!previousKey.signingPublicKey.contentEquals(activeMember.signingPublicKey) ||
                        !previousKey.encryptionPublicKey.contentEquals(activeMember.encryptionPublicKey)
                    ) {
                        return@forEach
                    }
                }
                packetsByContactId[activeMember.contactId] =
                    createMemberActivation(
                        groupId = packet.groupId,
                        epoch = packet.epoch,
                        activationId = packet.packetId,
                        activatedAtEpochMilliseconds = activationTimestamp,
                        activationRound = GroupMemberActivatedPacket.DISCOVERY_ROUND,
                        member = activatedMember,
                        recipientContactId = activeMember.contactId,
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
                member = activatedMember,
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

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String,
        transportMode: String
    ): Result<Unit> =
        runCatching {
            membershipLock.withLock {
                check(transportMode == SEALED_BOX_TRANSPORT_MODE) {
                    "Group member activation acknowledgement requires encrypted transport"
                }
                val securityState =
                    securityStore.findState(packet.groupId)
                        ?: error("Group security state was not found")
                check(securityState.currentEpoch == packet.epoch) {
                    "Group member activation acknowledgement uses the wrong epoch"
                }
                check(securityState.localRole.isGroupAdminRole()) {
                    "Only a group admin may receive member activation acknowledgements"
                }

                val memberKeys = securityStore.findMemberKeys(packet.groupId, packet.epoch)
                val acknowledgingMember =
                    memberKeys.firstOrNull { memberKey -> memberKey.contactId == acknowledgingContactId }
                        ?: error("Acknowledging group member was not found")
                check(
                    acknowledgingMember.signingPublicKey.contentEquals(
                        packet.acknowledgingMemberSigningPublicKey
                    )
                ) {
                    "Acknowledgement signing identity does not match the group member"
                }
                membershipPacketProtocol
                    .verifyMemberActivationAcknowledgement(
                        packet = packet,
                        expectedMemberSigningPublicKey = acknowledgingMember.signingPublicKey
                    ).getOrThrow()

                val activatedMember =
                    memberKeys.singleOrNull { memberKey ->
                        memberKey.signingPublicKey.contentEquals(packet.activatedMemberSigningPublicKey)
                    } ?: error("Activated group member was not found")
                check(activatedMember.contactId != acknowledgingContactId) {
                    "A group member cannot acknowledge its own activation"
                }

                sendActivationAcknowledgementFollowUp(
                    packet = packet,
                    acknowledgingMember = acknowledgingMember,
                    activatedMember = activatedMember
                )
            }
        }

    private suspend fun sendActivationAcknowledgementFollowUp(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingMember: GroupMemberKeyEntity,
        activatedMember: GroupMemberKeyEntity
    ) {
        val adminSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val acknowledgedAt = packet.acknowledgedAtEpochMilliseconds

        when (packet.activationRound) {
            GroupMemberActivatedPacket.DISCOVERY_ROUND ->
                packetBroadcaster
                    .enqueueAll(
                        mapOf(
                            activatedMember.contactId to
                                createMemberActivation(
                                    groupId = packet.groupId,
                                    epoch = packet.epoch,
                                    activationId = packet.activationId,
                                    activatedAtEpochMilliseconds = acknowledgedAt,
                                    activationRound = GroupMemberActivatedPacket.RECIPROCAL_ROUND,
                                    member = acknowledgingMember,
                                    recipientContactId = activatedMember.contactId,
                                    ownerSigningKeyPair = adminSigningKeyPair
                                )
                        )
                    ).getOrThrow()

            GroupMemberActivatedPacket.RECIPROCAL_ROUND ->
                packetBroadcaster
                    .enqueueAll(
                        linkedMapOf(
                            acknowledgingMember.contactId to
                                createMemberActivation(
                                    groupId = packet.groupId,
                                    epoch = packet.epoch,
                                    activationId = packet.activationId,
                                    activatedAtEpochMilliseconds = acknowledgedAt,
                                    activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                                    member = activatedMember,
                                    recipientContactId = acknowledgingMember.contactId,
                                    ownerSigningKeyPair = adminSigningKeyPair
                                ),
                            activatedMember.contactId to
                                createMemberActivation(
                                    groupId = packet.groupId,
                                    epoch = packet.epoch,
                                    activationId = packet.activationId,
                                    activatedAtEpochMilliseconds = acknowledgedAt,
                                    activationRound = GroupMemberActivatedPacket.FINAL_ROUND,
                                    member = acknowledgingMember,
                                    recipientContactId = activatedMember.contactId,
                                    ownerSigningKeyPair = adminSigningKeyPair
                                )
                        )
                    ).getOrThrow()

            else -> error("Unsupported member activation acknowledgement round")
        }
    }

    private suspend fun createMemberActivation(
        groupId: String,
        epoch: Int,
        activationId: String,
        activatedAtEpochMilliseconds: Long,
        activationRound: Int,
        member: GroupMemberKeyEntity,
        recipientContactId: String,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): GroupMemberActivatedPacket =
        membershipPacketProtocol
            .createMemberActivated(
                groupId = groupId,
                epoch = epoch,
                member =
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                        signingPublicKey = member.signingPublicKey.copyOf(),
                        role = member.role,
                        phoneNumber = member.phoneNumber
                    ),
                activatedAtEpochMilliseconds = activatedAtEpochMilliseconds,
                activationRound = activationRound,
                activationId = activationId,
                memberReferenceId = member.contactId,
                recipientContactId = recipientContactId,
                ownerSigningKeyPair = ownerSigningKeyPair
            ).getOrThrow()

    private companion object {
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
