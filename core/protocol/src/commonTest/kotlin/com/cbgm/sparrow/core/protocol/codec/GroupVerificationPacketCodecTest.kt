package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.packet.GroupVerificationMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.sparrow.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupVerificationPacketCodecTest {
    private val codec = KotlinxPacketCodec(json = createProtocolJson())

    @Test
    fun verificationReceiptRoundTrip() {
        val original =
            GroupVerificationReceiptPacket(
                packetId = "group-verification-receipt-receipt-1",
                groupId = "group-1",
                invitationId = "invite-1",
                receiptId = "receipt-1",
                verifiedAtEpochMilliseconds = 100L,
                participantEncryptionPublicKey = ByteArray(32) { 1 },
                participantSigningPublicKey = ByteArray(32) { 2 },
                ownerEncryptionPublicKey = ByteArray(32) { 3 },
                ownerSigningPublicKey = ByteArray(32) { 4 },
                signature = ByteArray(64) { 5 }
            )

        val packet =
            assertIs<GroupVerificationReceiptPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.invitationId, packet.invitationId)
        assertEquals(original.receiptId, packet.receiptId)
        assertContentEquals(original.participantEncryptionPublicKey, packet.participantEncryptionPublicKey)
        assertContentEquals(original.participantSigningPublicKey, packet.participantSigningPublicKey)
        assertContentEquals(original.ownerEncryptionPublicKey, packet.ownerEncryptionPublicKey)
        assertContentEquals(original.ownerSigningPublicKey, packet.ownerSigningPublicKey)
        assertContentEquals(original.signature, packet.signature)
    }

    @Test
    fun verificationSnapshotRequestRoundTrip() {
        val original =
            GroupVerificationSnapshotRequestPacket(
                packetId = "group-verification-snapshot-request-request-1",
                groupId = "group-1",
                invitationId = "invite-1",
                requestId = "request-1",
                requestedAtEpochMilliseconds = 100L,
                requesterSigningPublicKey = ByteArray(32) { 1 },
                signature = ByteArray(64) { 2 }
            )

        val packet =
            assertIs<GroupVerificationSnapshotRequestPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.invitationId, packet.invitationId)
        assertEquals(original.requestId, packet.requestId)
        assertContentEquals(original.requesterSigningPublicKey, packet.requesterSigningPublicKey)
        assertContentEquals(original.signature, packet.signature)
    }

    @Test
    fun verificationSnapshotRoundTrip() {
        val original =
            GroupVerificationSnapshotPacket(
                packetId = "group-verification-snapshot-snapshot-1-contact-1",
                groupId = "group-1",
                snapshotId = "snapshot-1",
                generatedAtEpochMilliseconds = 100L,
                ownerEncryptionPublicKey = ByteArray(32) { 1 },
                ownerSigningPublicKey = ByteArray(32) { 2 },
                members =
                    listOf(
                        GroupVerificationMemberPayload(
                            invitationId = "invite-1",
                            displayName = "Anna",
                            membershipStatus = GroupVerificationMemberPayload.ACTIVE_STATUS,
                            adminVerifiedParticipant = true,
                            participantVerifiedAdmin = false
                        ),
                        GroupVerificationMemberPayload(
                            invitationId = "invite-2",
                            displayName = "Bob",
                            membershipStatus = GroupVerificationMemberPayload.PENDING_STATUS,
                            adminVerifiedParticipant = false,
                            participantVerifiedAdmin = false
                        )
                    ),
                signature = ByteArray(64) { 3 }
            )

        val packet =
            assertIs<GroupVerificationSnapshotPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.snapshotId, packet.snapshotId)
        assertEquals(original.members, packet.members)
        assertContentEquals(original.ownerEncryptionPublicKey, packet.ownerEncryptionPublicKey)
        assertContentEquals(original.ownerSigningPublicKey, packet.ownerSigningPublicKey)
        assertContentEquals(original.signature, packet.signature)
    }
}
