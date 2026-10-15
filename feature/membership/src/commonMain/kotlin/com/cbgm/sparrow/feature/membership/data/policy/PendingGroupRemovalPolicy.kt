package com.cbgm.sparrow.feature.membership.data.policy

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

/** Pending cancellation is bound to the signing key saved with the verified invitation. */
internal object PendingGroupRemovalPolicy {
    fun signingKey(
        packet: GroupMemberRemovedPacket,
        senderContactId: String,
        membership: GroupMembershipEntity?
    ): ByteArray? {
        require(packet.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH)
        val pending = membership ?: return null
        check(pending.sourceInvitationId == packet.invitationId) { "Group removal references the wrong invitation" }
        check(pending.groupId == packet.groupId) { "Group removal references the wrong group" }
        check(pending.perspective == GroupMembershipPerspective.MEMBER.name) {
            "Pending group removal requires an incoming invitation"
        }
        check(pending.contactId == senderContactId) {
            "Pending group removal came from a contact that is not the inviter"
        }
        check(pending.challenge.contentEquals(packet.challenge)) {
            "Group removal membership challenge does not match"
        }
        if (pending.status == GroupMembershipStatus.REMOVED.name) return null
        check(
            pending.status == GroupMembershipStatus.STAGED.name ||
                pending.status == GroupMembershipStatus.JOIN_REQUEST_SENT.name
        ) {
            "An installed group key requires an epoch-advancing removal"
        }
        return checkNotNull(pending.ownerSigningPublicKey?.takeIf { it.isNotEmpty() }) {
            "Pending group invitation has no stored owner signing key"
        }.copyOf()
    }
}
