package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.ProfilePicturePayload
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

class GroupProtocolPayloadEncoderTest {
    private val encoder = GroupProtocolPayloadEncoder()

    @Test
    fun invitationEncodingIgnoresSignatureAndBindsBothIdentities() {
        val invite =
            GroupInvitePacket(
                packetId = "group-invite-1",
                invitationId = "invite-1",
                groupId = "group-1",
                title = "Friends",
                createdAtEpochMilliseconds = 100L,
                expiresAtEpochMilliseconds = 200L,
                challenge = byteArrayOf(1),
                ownerEncryptionPublicKey = byteArrayOf(2),
                ownerSigningPublicKey = byteArrayOf(3),
                ownerSignature = byteArrayOf(4)
            )
        val joinRequest =
            GroupJoinRequestPacket(
                packetId = "group-join-1",
                invitationId = invite.invitationId,
                groupId = invite.groupId,
                challenge = invite.challenge,
                memberEncryptionPublicKey = byteArrayOf(5),
                memberSigningPublicKey = byteArrayOf(6),
                memberSignature = byteArrayOf(7)
            )

        assertContentEquals(
            encoder.encodeInvite(invite),
            encoder.encodeInvite(invite.copy(ownerSignature = byteArrayOf(99)))
        )
        assertFalse(
            encoder
                .encodeInvite(invite)
                .contentEquals(encoder.encodeInvite(invite.copy(challenge = byteArrayOf(9))))
        )
        assertContentEquals(
            encoder.encodeJoinRequest(joinRequest),
            encoder.encodeJoinRequest(joinRequest.copy(memberSignature = byteArrayOf(99)))
        )
        assertFalse(
            encoder
                .encodeJoinRequest(joinRequest)
                .contentEquals(
                    encoder.encodeJoinRequest(
                        joinRequest.copy(memberSigningPublicKey = byteArrayOf(8))
                    )
                )
        )
        assertFalse(
            encoder
                .encodeInvite(invite)
                .contentEquals(
                    encoder.encodeInvite(
                        invite.copy(
                            profilePicture =
                                ProfilePictureMetadata(
                                    changedAtEpochMilliseconds = 10L,
                                    hasPicture = true,
                                    payload = ProfilePicturePayload(byteArrayOf(10))
                                )
                        )
                    )
                )
        )
        assertFalse(
            encoder
                .encodeJoinRequest(joinRequest)
                .contentEquals(
                    encoder.encodeJoinRequest(
                        joinRequest.copy(
                            profilePicture =
                                ProfilePictureMetadata(
                                    changedAtEpochMilliseconds = 11L,
                                    hasPicture = false
                                )
                        )
                    )
                )
        )
    }

    @Test
    fun deletionEncodingIgnoresSignatureAndBindsDeletionMetadata() {
        val deletion =
            GroupConversationDeletedPacket(
                packetId = "group-conversation-deleted-invite-1",
                invitationId = "invite-1",
                groupId = "group-1",
                epoch = 2,
                challenge = byteArrayOf(1),
                deletedAtEpochMilliseconds = 300L,
                ownerSignature = byteArrayOf(2)
            )

        assertContentEquals(
            encoder.encodeConversationDeleted(deletion),
            encoder.encodeConversationDeleted(deletion.copy(ownerSignature = byteArrayOf(99)))
        )
        assertFalse(
            encoder
                .encodeConversationDeleted(deletion)
                .contentEquals(
                    encoder.encodeConversationDeleted(
                        deletion.copy(deletedAtEpochMilliseconds = 301L)
                    )
                )
        )
    }

    @Test
    fun messageDeletionEncodingUsesSeparateDomainAndBindsMetadata() {
        val associatedData =
            encoder.encodeMessageDeletionAssociatedData(
                version = 1,
                groupId = "group-1",
                epoch = 2,
                deletionId = "delete-1",
                deletedAtEpochMilliseconds = 300L
            )

        assertFalse(
            associatedData.contentEquals(
                encoder.encodeMessageDeletionAssociatedData(
                    version = 1,
                    groupId = "group-1",
                    epoch = 2,
                    deletionId = "delete-2",
                    deletedAtEpochMilliseconds = 300L
                )
            )
        )
        assertFalse(
            associatedData.contentEquals(
                encoder.encodeMessageAssociatedData(
                    version = 1,
                    groupId = "group-1",
                    epoch = 2,
                    messageId = "delete-1",
                    sentAtEpochMilliseconds = 300L
                )
            )
        )

        val nonce = byteArrayOf(1)
        val ciphertext = byteArrayOf(2)
        assertFalse(
            encoder
                .encodeMessageDeletionSignature(associatedData, nonce, ciphertext)
                .contentEquals(encoder.encodeMessageSignature(associatedData, nonce, ciphertext))
        )
    }

    @Test
    fun welcomeEncodingIsDeterministicAndBindsMembership() {
        val packet = createWelcomePacket()

        assertContentEquals(
            encoder.encodeWelcome(packet),
            encoder.encodeWelcome(packet.copy(ownerSignature = byteArrayOf(99)))
        )
        assertFalse(
            encoder
                .encodeWelcome(packet)
                .contentEquals(
                    encoder.encodeWelcome(
                        packet.copy(
                            members =
                                packet.members.mapIndexed { index, member ->
                                    if (index == 1) member.copy(role = "OWNER") else member
                                }
                        )
                    )
                )
        )
        assertFalse(
            encoder
                .encodeWelcome(packet)
                .contentEquals(
                    encoder.encodeWelcome(
                        packet.copy(
                            membershipChange =
                                GroupMembershipChangePayload(
                                    reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                                    memberSigningPublicKey = byteArrayOf(4)
                                )
                        )
                    )
                )
        )
    }

    @Test
    fun responseEncodingIgnoresSignatureAndBindsWelcome() {
        val decline =
            GroupInviteDeclinedPacket(
                packetId = "decline-1",
                invitationId = "invite-1",
                groupId = "group-1",
                challenge = byteArrayOf(1),
                memberSigningPublicKey = byteArrayOf(2),
                memberSignature = byteArrayOf(3)
            )
        val ready =
            GroupReadyAcknowledgementPacket(
                packetId = "ready-1",
                groupId = "group-1",
                epoch = 1,
                welcomePacketId = "welcome-1",
                keyConfirmation = byteArrayOf(5),
                memberSignature = byteArrayOf(4)
            )

        assertContentEquals(
            encoder.encodeInviteDeclined(decline),
            encoder.encodeInviteDeclined(decline.copy(memberSignature = byteArrayOf(99)))
        )
        assertFalse(
            encoder
                .encodeInviteDeclined(decline)
                .contentEquals(encoder.encodeInviteDeclined(decline.copy(challenge = byteArrayOf(9))))
        )
        assertContentEquals(
            encoder.encodeReadyAcknowledgement(ready),
            encoder.encodeReadyAcknowledgement(ready.copy(memberSignature = byteArrayOf(99)))
        )
        assertFalse(
            encoder
                .encodeReadyAcknowledgement(ready)
                .contentEquals(
                    encoder.encodeReadyAcknowledgement(ready.copy(welcomePacketId = "welcome-2"))
                )
        )
        assertFalse(
            encoder
                .encodeReadyAcknowledgement(ready)
                .contentEquals(
                    encoder.encodeReadyAcknowledgement(
                        ready.copy(keyConfirmation = byteArrayOf(6))
                    )
                )
        )
    }

    @Test
    fun messageEncodingBindsEpochTimestampNonceAndCiphertext() {
        val associatedData =
            encoder.encodeMessageAssociatedData(
                version = 1,
                groupId = "group-1",
                epoch = 2,
                messageId = "message-1",
                sentAtEpochMilliseconds = 123L
            )
        val signature =
            encoder.encodeMessageSignature(
                associatedData = associatedData,
                nonce = byteArrayOf(1, 2),
                ciphertext = byteArrayOf(3, 4)
            )
        val changedEpoch =
            encoder.encodeMessageAssociatedData(
                version = 1,
                groupId = "group-1",
                epoch = 3,
                messageId = "message-1",
                sentAtEpochMilliseconds = 123L
            )
        val changedProfile =
            encoder.encodeMessageAssociatedData(
                version = 1,
                groupId = "group-1",
                epoch = 2,
                messageId = "message-1",
                sentAtEpochMilliseconds = 123L,
                profilePicture =
                    ProfilePictureMetadata(
                        changedAtEpochMilliseconds = 10L,
                        hasPicture = true,
                        payload = ProfilePicturePayload(byteArrayOf(7))
                    )
            )
        val changedCiphertext =
            encoder.encodeMessageSignature(
                associatedData = associatedData,
                nonce = byteArrayOf(1, 2),
                ciphertext = byteArrayOf(3, 5)
            )

        assertFalse(associatedData.contentEquals(changedEpoch))
        assertFalse(associatedData.contentEquals(changedProfile))
        assertFalse(signature.contentEquals(changedCiphertext))
    }

    private fun createWelcomePacket(): GroupCreatedPacket =
        GroupCreatedPacket(
            packetId = "welcome-1",
            groupId = "group-1",
            title = "Security",
            createdAtEpochMilliseconds = 123L,
            epoch = 1,
            members =
                listOf(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = byteArrayOf(1),
                        signingPublicKey = byteArrayOf(2),
                        role = "OWNER",
                        phoneNumber = "+491"
                    ),
                    GroupMemberPayload(
                        displayName = "Member",
                        encryptionPublicKey = byteArrayOf(3),
                        signingPublicKey = byteArrayOf(4),
                        role = "MEMBER",
                        phoneNumber = "+492"
                    )
                ),
            wrappedGroupKey = byteArrayOf(5, 6),
            ownerSignature = byteArrayOf(7, 8)
        )
}
