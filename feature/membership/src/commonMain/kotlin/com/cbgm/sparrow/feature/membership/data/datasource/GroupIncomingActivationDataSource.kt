package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.membership.data.GroupMembershipEvent
import com.cbgm.sparrow.feature.membership.data.GroupMembershipLock
import com.cbgm.sparrow.feature.membership.data.GroupMembershipStateMachine
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.data.protocol.GroupMembershipPacketProtocol

/** Validates and applies only Membership-owned state for an incoming activation. */
internal class GroupIncomingActivationDataSource(
    private val securityStore: GroupSecurityStoreDataSource,
    private val membershipStore: GroupMembershipStoreDataSource,
    private val membershipProtocol: GroupMembershipPacketProtocol,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val lock: GroupMembershipLock
) {
    suspend fun authorize(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String
    ): Boolean {
        check(transportMode == "SEALED_BOX") {
            "Group member activation requires encrypted transport"
        }
        val state = securityStore.findState(packet.groupId)
            ?: error("Group security state was not found")
        check(state.currentEpoch == packet.epoch) {
            "Group member activation uses the wrong epoch"
        }
        val admin = securityStore.findMemberKey(packet.groupId, state.currentEpoch, ownerContactId)
            ?: error("Group update sender is not part of the current epoch")
        check(admin.role.isGroupAdminRole()) { "Group update sender is not an admin" }
        check(admin.signingPublicKey.contentEquals(ownerSigningPublicKey)) {
            "Group admin signing identity changed"
        }
        membershipProtocol.verifyMemberActivated(packet, ownerSigningPublicKey).getOrThrow()
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        return packet.member.encryptionPublicKey.contentEquals(localIdentity.encryptionPublicKey) &&
            packet.member.signingPublicKey.contentEquals(localIdentity.signingPublicKey)
    }

    suspend fun apply(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String,
        memberContactId: String?,
        receivedAtEpochMilliseconds: Long
    ): Boolean = lock.withLock {
        val local = authorize(packet, ownerContactId, ownerSigningPublicKey, transportMode)
        if (local) {
            check(packet.activationRound == GroupMemberActivatedPacket.FINAL_ROUND) {
                "Local group membership requires a final activation"
            }
            val membership = membershipStore.findByGroupAndContact(packet.groupId, ownerContactId)
                ?: return@withLock true
            if (membership.status == GroupMembershipStatus.ACTIVE.name) return@withLock true
            check(membership.status == GroupMembershipStatus.WAITING_FOR_ACTIVATION.name) {
                "Group membership was activated from status ${membership.status}"
            }
            val updated = membershipStore.updateStatus(
                membershipId = membership.membershipId,
                expectedStatus = GroupMembershipStatus.WAITING_FOR_ACTIVATION.name,
                newStatus = GroupMembershipStateMachine.transition(
                    membership.status,
                    GroupMembershipEvent.MEMBER_ACTIVATED
                ).name,
                updatedAt = maxOf(
                    membership.createdAtEpochMilliseconds,
                    packet.activatedAtEpochMilliseconds,
                    receivedAtEpochMilliseconds
                )
            )
            check(updated == 1) { "Group membership changed while activation was applied" }
            return@withLock true
        }

        val contactId = requireNotNull(memberContactId) { "Activated group member contact was not resolved" }
        val oldKey = securityStore.findMemberKey(packet.groupId, packet.epoch, contactId)
        if (oldKey != null) {
            check(oldKey.encryptionPublicKey.contentEquals(packet.member.encryptionPublicKey)) {
                "Activated group member encryption key changed"
            }
            check(oldKey.signingPublicKey.contentEquals(packet.member.signingPublicKey)) {
                "Activated group member signing key changed"
            }
        }
        securityStore.upsertMemberKey(
            GroupMemberKeyEntity(
                groupId = packet.groupId,
                epoch = packet.epoch,
                contactId = contactId,
                encryptionPublicKey = packet.member.encryptionPublicKey.copyOf(),
                signingPublicKey = packet.member.signingPublicKey.copyOf(),
                role = packet.member.role,
                phoneNumber = packet.member.phoneNumber ?: oldKey?.phoneNumber
            )
        )
        if (packet.activationRound > GroupMemberActivatedPacket.FINAL_ROUND) {
            val acknowledgement = membershipProtocol.createMemberActivationAcknowledgement(
                activationPacket = packet,
                acknowledgedAtEpochMilliseconds = receivedAtEpochMilliseconds,
                memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            ).getOrThrow()
            protocolOutbox.enqueue(ownerContactId, acknowledgement).getOrThrow()
        }
        false
    }
}
