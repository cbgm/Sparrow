package com.cbgm.sparrow.feature.chats.data.group.protocol

import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarMetadata
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata

class GroupProtocolPayloadEncoder {
    fun encodeAvatarUpdated(packet: GroupAvatarUpdatedPacket): ByteArray =
        ByteArrays.concatenate(
            AVATAR_UPDATED_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            encodeGroupAvatar(packet.avatar),
            ByteArrays.withLengthPrefix(packet.adminSigningPublicKey)
        )

    fun encodeConversationDeleted(packet: GroupConversationDeletedPacket): ByteArray =
        ByteArrays.concatenate(
            CONVERSATION_DELETED_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.encodeLong(packet.deletedAtEpochMilliseconds)
        )

    fun encodeInvite(packet: GroupInvitePacket): ByteArray =
        ByteArrays.concatenate(
            INVITE_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            encodeString(packet.title),
            ByteArrays.encodeLong(packet.createdAtEpochMilliseconds),
            ByteArrays.encodeLong(packet.expiresAtEpochMilliseconds),
            encodeProfilePicture(packet.profilePicture),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.ownerEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.ownerSigningPublicKey)
        )

    fun encodeInviteReceived(packet: GroupInviteReceivedPacket): ByteArray =
        ByteArrays.concatenate(
            INVITE_RECEIVED_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.memberSigningPublicKey),
            ByteArrays.encodeLong(packet.receivedAtEpochMilliseconds)
        )

    fun encodeJoinRequest(packet: GroupJoinRequestPacket): ByteArray =
        ByteArrays.concatenate(
            JOIN_REQUEST_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            encodeProfilePicture(packet.profilePicture),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.memberEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.memberSigningPublicKey)
        )

    fun encodeInviteDeclined(packet: GroupInviteDeclinedPacket): ByteArray =
        ByteArrays.concatenate(
            INVITE_DECLINED_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.memberSigningPublicKey)
        )

    fun encodeLeaveRequest(packet: GroupLeaveRequestPacket): ByteArray =
        ByteArrays.concatenate(
            LEAVE_REQUEST_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.memberSigningPublicKey),
            ByteArrays.encodeLong(packet.requestedAtEpochMilliseconds)
        )

    fun encodeReadyAcknowledgement(packet: GroupReadyAcknowledgementPacket): ByteArray =
        ByteArrays.concatenate(
            READY_ACKNOWLEDGEMENT_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            encodeString(packet.welcomePacketId),
            ByteArrays.withLengthPrefix(packet.keyConfirmation)
        )

    fun encodeMemberActivated(packet: GroupMemberActivatedPacket): ByteArray =
        ByteArrays.concatenate(
            MEMBER_ACTIVATED_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            encodeString(packet.activationId),
            encodeMembers(listOf(packet.member)),
            ByteArrays.encodeLong(packet.activatedAtEpochMilliseconds),
            ByteArrays.encodeInt(packet.activationRound)
        )

    fun encodeMemberActivationAcknowledgement(packet: GroupMemberActivationAcknowledgementPacket): ByteArray =
        ByteArrays.concatenate(
            MEMBER_ACTIVATION_ACKNOWLEDGEMENT_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            encodeString(packet.activationPacketId),
            encodeString(packet.activationId),
            ByteArrays.encodeInt(packet.activationRound),
            ByteArrays.withLengthPrefix(packet.activatedMemberSigningPublicKey),
            ByteArrays.withLengthPrefix(packet.acknowledgingMemberSigningPublicKey),
            ByteArrays.encodeLong(packet.acknowledgedAtEpochMilliseconds)
        )

    fun encodeMemberRemoved(packet: GroupMemberRemovedPacket): ByteArray =
        ByteArrays.concatenate(
            if (packet.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                MEMBER_LEFT_DOMAIN
            } else {
                MEMBER_REMOVED_DOMAIN
            },
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            if (packet.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                encodeString(packet.reason)
            } else {
                byteArrayOf()
            },
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.removedMemberSigningPublicKey),
            ByteArrays.encodeLong(packet.removedAtEpochMilliseconds)
        )

    fun encodeWelcome(packet: GroupCreatedPacket): ByteArray =
        ByteArrays.concatenate(
            WELCOME_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            encodeString(packet.title),
            ByteArrays.encodeLong(packet.createdAtEpochMilliseconds),
            ByteArrays.encodeInt(packet.epoch),
            encodeMembers(packet.members),
            ByteArrays.withLengthPrefix(packet.wrappedGroupKey),
            packet.membershipChange?.let(::encodeMembershipChange) ?: byteArrayOf()
        )

    fun encodeMessageAssociatedData(
        version: Int,
        groupId: String,
        epoch: Int,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        profilePicture: ProfilePictureMetadata = ProfilePictureMetadata()
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_ASSOCIATED_DATA_DOMAIN,
            ByteArrays.encodeInt(version),
            encodeString(groupId),
            ByteArrays.encodeInt(epoch),
            encodeString(messageId),
            ByteArrays.encodeLong(sentAtEpochMilliseconds),
            encodeProfilePicture(profilePicture)
        )

    fun encodeMessageSignature(
        associatedData: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_SIGNATURE_DOMAIN,
            ByteArrays.withLengthPrefix(associatedData),
            ByteArrays.withLengthPrefix(nonce),
            ByteArrays.withLengthPrefix(ciphertext)
        )

    fun encodeMessageDeletionAssociatedData(
        version: Int,
        groupId: String,
        epoch: Int,
        deletionId: String,
        deletedAtEpochMilliseconds: Long
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_DELETION_ASSOCIATED_DATA_DOMAIN,
            ByteArrays.encodeInt(version),
            encodeString(groupId),
            ByteArrays.encodeInt(epoch),
            encodeString(deletionId),
            ByteArrays.encodeLong(deletedAtEpochMilliseconds)
        )

    fun encodeMessageDeletionSignature(
        associatedData: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_DELETION_SIGNATURE_DOMAIN,
            ByteArrays.withLengthPrefix(associatedData),
            ByteArrays.withLengthPrefix(nonce),
            ByteArrays.withLengthPrefix(ciphertext)
        )

    fun encodeMessageEditAssociatedData(
        version: Int,
        groupId: String,
        epoch: Int,
        editId: String,
        editedAtEpochMilliseconds: Long
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_EDIT_ASSOCIATED_DATA_DOMAIN,
            ByteArrays.encodeInt(version),
            encodeString(groupId),
            ByteArrays.encodeInt(epoch),
            encodeString(editId),
            ByteArrays.encodeLong(editedAtEpochMilliseconds)
        )

    fun encodeMessageEditSignature(
        associatedData: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_EDIT_SIGNATURE_DOMAIN,
            ByteArrays.withLengthPrefix(associatedData),
            ByteArrays.withLengthPrefix(nonce),
            ByteArrays.withLengthPrefix(ciphertext)
        )

    private fun encodeGroupAvatar(metadata: GroupAvatarMetadata): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.encodeLong(metadata.changedAtEpochMilliseconds),
            byteArrayOf(if (metadata.hasAvatar) 1 else 0),
            ByteArrays.withLengthPrefix(metadata.payload?.bytes ?: byteArrayOf())
        )

    private fun encodeProfilePicture(metadata: ProfilePictureMetadata): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.encodeLong(metadata.changedAtEpochMilliseconds),
            byteArrayOf(if (metadata.hasPicture) 1 else 0),
            ByteArrays.withLengthPrefix(metadata.payload?.bytes ?: byteArrayOf())
        )

    private fun encodeMembers(members: List<GroupMemberPayload>): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.encodeInt(members.size),
            *members
                .map { member ->
                    ByteArrays.concatenate(
                        encodeNullableString(member.displayName),
                        ByteArrays.withLengthPrefix(member.encryptionPublicKey),
                        ByteArrays.withLengthPrefix(member.signingPublicKey),
                        encodeString(member.role),
                        encodeNullableString(member.phoneNumber)
                    )
                }.toTypedArray()
        )

    private fun encodeMembershipChange(change: GroupMembershipChangePayload): ByteArray =
        ByteArrays.concatenate(
            byteArrayOf(NON_NULL_VALUE),
            encodeString(change.reason),
            ByteArrays.withLengthPrefix(change.memberSigningPublicKey)
        )

    private fun encodeString(value: String): ByteArray = ByteArrays.withLengthPrefix(value.encodeToByteArray())

    private fun encodeNullableString(value: String?): ByteArray =
        if (value == null) {
            byteArrayOf(NULL_VALUE)
        } else {
            ByteArrays.concatenate(
                byteArrayOf(NON_NULL_VALUE),
                encodeString(value)
            )
        }

    private companion object {
        val AVATAR_UPDATED_DOMAIN = "sparrow.group-avatar-updated.v1".encodeToByteArray()
        val MEMBER_ACTIVATED_DOMAIN = "sparrow.group-member-activated.v1".encodeToByteArray()
        val MEMBER_ACTIVATION_ACKNOWLEDGEMENT_DOMAIN =
            "sparrow.group-member-activation-acknowledgement.v1".encodeToByteArray()
        val MEMBER_REMOVED_DOMAIN = "sparrow.group-member-removed.v1".encodeToByteArray()
        val MEMBER_LEFT_DOMAIN = "sparrow.group-member-left.v1".encodeToByteArray()
        val CONVERSATION_DELETED_DOMAIN = "sparrow.group-conversation-deleted.v1".encodeToByteArray()
        val LEAVE_REQUEST_DOMAIN = "sparrow.group-leave-request.v1".encodeToByteArray()
        val INVITE_DOMAIN = "sparrow.group-invite.v1".encodeToByteArray()
        val INVITE_RECEIVED_DOMAIN = "sparrow.group-invite-received.v1".encodeToByteArray()
        val JOIN_REQUEST_DOMAIN = "sparrow.group-join-request.v1".encodeToByteArray()
        val INVITE_DECLINED_DOMAIN = "sparrow.group-invite-declined.v1".encodeToByteArray()
        val READY_ACKNOWLEDGEMENT_DOMAIN = "sparrow.group-ready-acknowledgement.v1".encodeToByteArray()
        val WELCOME_DOMAIN = "sparrow.group-welcome.v1".encodeToByteArray()
        val MESSAGE_ASSOCIATED_DATA_DOMAIN = "sparrow.group-message.aad.v1".encodeToByteArray()
        val MESSAGE_SIGNATURE_DOMAIN = "sparrow.group-message.signature.v1".encodeToByteArray()
        val MESSAGE_DELETION_ASSOCIATED_DATA_DOMAIN =
            "sparrow.group-message-deletion.aad.v1".encodeToByteArray()
        val MESSAGE_DELETION_SIGNATURE_DOMAIN =
            "sparrow.group-message-deletion.signature.v1".encodeToByteArray()
        val MESSAGE_EDIT_ASSOCIATED_DATA_DOMAIN =
            "sparrow.group-message-edit.aad.v1".encodeToByteArray()
        val MESSAGE_EDIT_SIGNATURE_DOMAIN =
            "sparrow.group-message-edit.signature.v1".encodeToByteArray()

        const val NULL_VALUE: Byte = 0
        const val NON_NULL_VALUE: Byte = 1
    }
}
