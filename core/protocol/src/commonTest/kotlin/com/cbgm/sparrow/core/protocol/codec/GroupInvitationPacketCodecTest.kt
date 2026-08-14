package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupInvitationPacketCodecTest {
    private val codec = KotlinxPacketCodec(json = createProtocolJson())

    @Test
    fun groupInviteRoundTrip() {
        val original =
            GroupInvitePacket(
                packetId = "group-invite-1",
                invitationId = "invite-1",
                groupId = "group-1",
                title = "Friends",
                createdAtEpochMilliseconds = 100L,
                expiresAtEpochMilliseconds = 200L,
                challenge = byteArrayOf(1, 2),
                ownerEncryptionPublicKey = byteArrayOf(3, 4),
                ownerSigningPublicKey = byteArrayOf(5, 6),
                ownerSignature = byteArrayOf(7, 8)
            )

        val packet = assertIs<GroupInvitePacket>(codec.decode(codec.encode(original).getOrThrow()).getOrThrow())

        assertEquals(original.packetId, packet.packetId)
        assertEquals(original.invitationId, packet.invitationId)
        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.title, packet.title)
        assertContentEquals(original.challenge, packet.challenge)
        assertContentEquals(original.ownerEncryptionPublicKey, packet.ownerEncryptionPublicKey)
        assertContentEquals(original.ownerSigningPublicKey, packet.ownerSigningPublicKey)
        assertContentEquals(original.ownerSignature, packet.ownerSignature)
    }

    @Test
    fun groupInviteReceivedRoundTrip() {
        val original =
            GroupInviteReceivedPacket(
                packetId = "group-invite-received-invite-1",
                invitationId = "invite-1",
                groupId = "group-1",
                challenge = byteArrayOf(1, 2),
                memberSigningPublicKey = byteArrayOf(3, 4),
                receivedAtEpochMilliseconds = 150L,
                memberSignature = byteArrayOf(5, 6)
            )

        val packet =
            assertIs<GroupInviteReceivedPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original, packet)
    }

    @Test
    fun groupConversationDeletedRoundTrip() {
        val original =
            GroupConversationDeletedPacket(
                packetId = "group-conversation-deleted-invite-1",
                invitationId = "invite-1",
                groupId = "group-1",
                epoch = 2,
                challenge = byteArrayOf(1, 2),
                deletedAtEpochMilliseconds = 300L,
                ownerSignature = byteArrayOf(3, 4)
            )

        val packet =
            assertIs<GroupConversationDeletedPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original, packet)
    }

    @Test
    fun groupJoinRequestRoundTrip() {
        val original =
            GroupJoinRequestPacket(
                packetId = "group-join-1",
                invitationId = "invite-1",
                groupId = "group-1",
                challenge = byteArrayOf(1, 2),
                memberEncryptionPublicKey = byteArrayOf(3, 4),
                memberSigningPublicKey = byteArrayOf(5, 6),
                memberSignature = byteArrayOf(7, 8)
            )

        val packet =
            assertIs<GroupJoinRequestPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.packetId, packet.packetId)
        assertEquals(original.invitationId, packet.invitationId)
        assertEquals(original.groupId, packet.groupId)
        assertContentEquals(original.challenge, packet.challenge)
        assertContentEquals(original.memberEncryptionPublicKey, packet.memberEncryptionPublicKey)
        assertContentEquals(original.memberSigningPublicKey, packet.memberSigningPublicKey)
        assertContentEquals(original.memberSignature, packet.memberSignature)
    }

    @Test
    fun groupInviteDeclinedRoundTrip() {
        val original =
            GroupInviteDeclinedPacket(
                packetId = "group-decline-1",
                invitationId = "invite-1",
                groupId = "group-1",
                challenge = byteArrayOf(1, 2),
                memberSigningPublicKey = byteArrayOf(3, 4),
                memberSignature = byteArrayOf(5, 6)
            )

        val packet =
            assertIs<GroupInviteDeclinedPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.invitationId, packet.invitationId)
        assertEquals(original.groupId, packet.groupId)
        assertContentEquals(original.challenge, packet.challenge)
        assertContentEquals(original.memberSigningPublicKey, packet.memberSigningPublicKey)
        assertContentEquals(original.memberSignature, packet.memberSignature)
    }

    @Test
    fun groupReadyAcknowledgementRoundTrip() {
        val original =
            GroupReadyAcknowledgementPacket(
                packetId = "group-ready-1",
                groupId = "group-1",
                epoch = 1,
                welcomePacketId = "welcome-1",
                keyConfirmation = byteArrayOf(3, 4),
                memberSignature = byteArrayOf(1, 2)
            )

        val packet =
            assertIs<GroupReadyAcknowledgementPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.epoch, packet.epoch)
        assertEquals(original.welcomePacketId, packet.welcomePacketId)
        assertContentEquals(original.keyConfirmation, packet.keyConfirmation)
        assertContentEquals(original.memberSignature, packet.memberSignature)
    }

    @Test
    fun groupMemberRemovedRoundTrip() {
        val original =
            GroupMemberRemovedPacket(
                packetId = "group-member-removed-invite-1-2",
                invitationId = "invite-1",
                groupId = "group-1",
                epoch = 2,
                challenge = byteArrayOf(1, 2),
                removedMemberSigningPublicKey = byteArrayOf(3, 4),
                removedAtEpochMilliseconds = 300L,
                ownerSignature = byteArrayOf(5, 6)
            )

        val packet =
            assertIs<GroupMemberRemovedPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original, packet)
    }

    @Test
    fun groupLeaveRequestRoundTrip() {
        val original =
            GroupLeaveRequestPacket(
                packetId = "group-leave-invite-1-2",
                invitationId = "invite-1",
                groupId = "group-1",
                epoch = 2,
                challenge = byteArrayOf(1, 2),
                memberSigningPublicKey = byteArrayOf(3, 4),
                requestedAtEpochMilliseconds = 300L,
                memberSignature = byteArrayOf(5, 6)
            )

        val packet =
            assertIs<GroupLeaveRequestPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original, packet)
    }
}
