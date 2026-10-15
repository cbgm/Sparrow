package com.cbgm.sparrow.feature.membership.data.policy

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PendingGroupRemovalPolicyTest {
    private val ownerKey = ByteArray(32) { 7 }
    private val challenge = ByteArray(32) { 3 }
    private val membership = GroupMembershipEntity(
        membershipId = "membership",
        sourceInvitationId = "invitation",
        groupId = "group",
        contactId = "inviter",
        perspective = GroupMembershipPerspective.MEMBER.name,
        status = GroupMembershipStatus.STAGED.name,
        challenge = challenge,
        ownerSigningPublicKey = ownerKey,
        createdAtEpochMilliseconds = 1L,
        updatedAtEpochMilliseconds = 1L
    )
    private val packet = GroupMemberRemovedPacket(
        packetId = "removal",
        invitationId = "invitation",
        groupId = "group",
        epoch = 0,
        challenge = challenge,
        removedMemberSigningPublicKey = byteArrayOf(),
        removedAtEpochMilliseconds = 2L,
        ownerSignature = byteArrayOf(1)
    )

    @Test
    fun pendingRemovalUsesInvitationKeyWithoutAContactIdentityRecord() {
        for (status in listOf(GroupMembershipStatus.STAGED, GroupMembershipStatus.JOIN_REQUEST_SENT)) {
            val key = PendingGroupRemovalPolicy.signingKey(packet, "inviter", membership.copy(status = status.name))
            assertContentEquals(ownerKey, key)
        }
    }

    @Test
    fun removalMustMatchTheInvitationGroupSenderAndChallenge() {
        val invalidRows = listOf(
            membership.copy(sourceInvitationId = "other-invitation"),
            membership.copy(groupId = "other-group"),
            membership.copy(contactId = "other-contact"),
            membership.copy(challenge = byteArrayOf(99)),
            membership.copy(perspective = GroupMembershipPerspective.OWNER.name)
        )
        invalidRows.forEach { row ->
            assertFailsWith<IllegalStateException> { PendingGroupRemovalPolicy.signingKey(packet, "inviter", row) }
        }
    }

    @Test
    fun epochZeroCannotRemoveAnInstalledMembership() {
        for (status in listOf(
            GroupMembershipStatus.WAITING_FOR_ACTIVATION,
            GroupMembershipStatus.ACTIVE,
            GroupMembershipStatus.LEAVE_REQUESTED,
            GroupMembershipStatus.WELCOME_SENT
        )) {
            assertFailsWith<IllegalStateException> {
                PendingGroupRemovalPolicy.signingKey(packet, "inviter", membership.copy(status = status.name))
            }
        }
    }

    @Test
    fun unknownOrAlreadyRemovedInvitationIsNotApplicable() {
        assertEquals(null, PendingGroupRemovalPolicy.signingKey(packet, "inviter", null))
        assertEquals(
            null,
            PendingGroupRemovalPolicy.signingKey(
                packet,
                "inviter",
                membership.copy(status = GroupMembershipStatus.REMOVED.name, ownerSigningPublicKey = null)
            )
        )
    }

    @Test
    fun pendingInvitationWithoutAPinnedKeyIsRejected() {
        for (key in listOf(null, byteArrayOf())) {
            assertFailsWith<IllegalStateException> {
                PendingGroupRemovalPolicy.signingKey(packet, "inviter", membership.copy(ownerSigningPublicKey = key))
            }
        }
    }

    @Test
    fun returnedKeyCannotMutateTheStoredInvitationIdentity() {
        val resolved = PendingGroupRemovalPolicy.signingKey(packet, "inviter", membership)!!
        resolved.fill(0)
        assertContentEquals(ByteArray(32) { 7 }, membership.ownerSigningPublicKey)
    }
}
