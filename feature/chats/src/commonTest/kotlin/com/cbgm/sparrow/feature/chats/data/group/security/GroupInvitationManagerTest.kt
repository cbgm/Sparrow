package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.core.crypto.group.GroupCiphertext
import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupMembershipPacketProtocolTest {
    private val manager =
        GroupMembershipPacketProtocol(
            groupCrypto = DeterministicGroupCrypto(),
            payloadEncoder = GroupProtocolPayloadEncoder()
        )

    @Test
    fun createsSignedInviteAndRejectsChangedMetadata() =
        runTest {
            val invite =
                manager
                    .createInvite(
                        invitationId = "invite-1",
                        groupId = "group-1",
                        title = "Friends",
                        createdAtEpochMilliseconds = 100L,
                        expiresAtEpochMilliseconds = 200L,
                        ownerIdentity =
                            LocalPublicIdentity(
                                encryptionPublicKey = OWNER_ENCRYPTION_KEY,
                                signingPublicKey = OWNER_SIGNING_KEY
                            ),
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals("group-invite-invite-1", invite.packetId)
            assertContentEquals(CHALLENGE, invite.challenge)
            manager.verifyInvite(invite).getOrThrow()
            assertTrue(manager.verifyInvite(invite.copy(title = "Changed")).isFailure)
        }

    @Test
    fun groupDeletionBindsInvitationEpochAndTimestamp() =
        runTest {
            val deletion =
                manager
                    .createConversationDeleted(
                        invitationId = "invite-delete",
                        groupId = "group-delete",
                        epoch = 4,
                        challenge = CHALLENGE,
                        deletedAtEpochMilliseconds = 500L,
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()

            manager.verifyConversationDeleted(deletion, OWNER_SIGNING_KEY).getOrThrow()
            assertEquals("group-conversation-deleted-invite-delete", deletion.packetId)
            assertTrue(
                manager
                    .verifyConversationDeleted(
                        deletion.copy(deletedAtEpochMilliseconds = 501L),
                        OWNER_SIGNING_KEY
                    ).isFailure
            )
        }

    @Test
    fun joinRequestBindsInvitationAndMemberIdentity() =
        runTest {
            val invite =
                manager
                    .createInvite(
                        invitationId = "invite-2",
                        groupId = "group-2",
                        title = "Team",
                        createdAtEpochMilliseconds = 100L,
                        expiresAtEpochMilliseconds = 200L,
                        ownerIdentity =
                            LocalPublicIdentity(
                                encryptionPublicKey = OWNER_ENCRYPTION_KEY,
                                signingPublicKey = OWNER_SIGNING_KEY
                            ),
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()
            val joinRequest =
                manager
                    .createJoinRequest(
                        invite = invite,
                        memberIdentity =
                            LocalPublicIdentity(
                                encryptionPublicKey = MEMBER_ENCRYPTION_KEY,
                                signingPublicKey = MEMBER_SIGNING_KEY
                            ),
                        memberSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = MEMBER_SIGNING_KEY,
                                privateKey = MEMBER_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals("group-join-invite-2", joinRequest.packetId)
            assertContentEquals(CHALLENGE, joinRequest.challenge)
            manager.verifyJoinRequest(joinRequest).getOrThrow()
            assertTrue(
                manager
                    .verifyJoinRequest(joinRequest.copy(challenge = byteArrayOf(99)))
                    .isFailure
            )
        }

    @Test
    fun declineAndReadyAcknowledgementAreSigned() =
        runTest {
            val signingKeyPair =
                LocalSigningKeyPair(
                    publicKey = MEMBER_SIGNING_KEY,
                    privateKey = MEMBER_SIGNING_KEY
                )
            val decline =
                manager
                    .createDecline(
                        invitationId = "invite-3",
                        groupId = "group-3",
                        challenge = CHALLENGE,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()
            val ready =
                manager
                    .createReadyAcknowledgement(
                        groupId = "group-3",
                        epoch = 1,
                        welcomePacketId = "welcome-3",
                        keyConfirmation = byteArrayOf(5),
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            manager.verifyDecline(decline).getOrThrow()
            manager
                .verifyReadyAcknowledgement(ready, MEMBER_SIGNING_KEY)
                .getOrThrow()
            assertTrue(manager.verifyDecline(decline.copy(groupId = "group-4")).isFailure)
            assertTrue(
                manager
                    .verifyReadyAcknowledgement(
                        ready.copy(welcomePacketId = "welcome-4"),
                        MEMBER_SIGNING_KEY
                    ).isFailure
            )
            assertTrue(
                manager
                    .verifyReadyAcknowledgement(
                        ready.copy(keyConfirmation = byteArrayOf(6)),
                        MEMBER_SIGNING_KEY
                    ).isFailure
            )
        }

    @Test
    fun memberRemovalBindsInvitationEpochAndRemovedIdentity() =
        runTest {
            val removal =
                manager
                    .createMemberRemoved(
                        invitationId = "invite-4",
                        groupId = "group-4",
                        epoch = 2,
                        challenge = CHALLENGE,
                        removedMemberSigningPublicKey = MEMBER_SIGNING_KEY,
                        removedAtEpochMilliseconds = 300L,
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()

            manager.verifyMemberRemoved(removal, OWNER_SIGNING_KEY).getOrThrow()
            assertTrue(
                manager
                    .verifyMemberRemoved(removal.copy(epoch = 3), OWNER_SIGNING_KEY)
                    .isFailure
            )
            val voluntaryLeave =
                manager
                    .createMemberRemoved(
                        invitationId = "invite-4",
                        groupId = "group-4",
                        epoch = 2,
                        reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                        challenge = CHALLENGE,
                        removedMemberSigningPublicKey = MEMBER_SIGNING_KEY,
                        removedAtEpochMilliseconds = 300L,
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()

            manager.verifyMemberRemoved(voluntaryLeave, OWNER_SIGNING_KEY).getOrThrow()
            assertTrue(
                manager
                    .verifyMemberRemoved(
                        voluntaryLeave.copy(
                            reason = GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER
                        ),
                        OWNER_SIGNING_KEY
                    ).isFailure
            )
        }

    @Test
    fun leaveRequestBindsMembershipEpochAndMemberIdentity() =
        runTest {
            val request =
                manager
                    .createLeaveRequest(
                        invitationId = "invite-5",
                        groupId = "group-5",
                        epoch = 3,
                        challenge = CHALLENGE,
                        requestedAtEpochMilliseconds = 400L,
                        memberSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = MEMBER_SIGNING_KEY,
                                privateKey = MEMBER_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals("group-leave-invite-5-3", request.packetId)
            manager.verifyLeaveRequest(request, MEMBER_SIGNING_KEY).getOrThrow()
            assertTrue(
                manager
                    .verifyLeaveRequest(request.copy(epoch = 4), MEMBER_SIGNING_KEY)
                    .isFailure
            )
        }

    private class DeterministicGroupCrypto : GroupCrypto {
        override suspend fun generateGroupKey(): Result<ByteArray> = Result.success(ByteArray(32))

        override suspend fun generateInvitationChallenge(): Result<ByteArray> = Result.success(CHALLENGE.copyOf())

        override suspend fun wrapGroupKey(
            groupKey: ByteArray,
            recipientEncryptionPublicKey: ByteArray
        ): Result<ByteArray> = Result.success(groupKey + recipientEncryptionPublicKey)

        override suspend fun unwrapGroupKey(
            wrappedGroupKey: ByteArray,
            localEncryptionPublicKey: ByteArray,
            localEncryptionPrivateKey: ByteArray
        ): Result<ByteArray> = Result.success(wrappedGroupKey)

        override suspend fun encryptMessage(
            plaintext: ByteArray,
            associatedData: ByteArray,
            groupKey: ByteArray
        ): Result<GroupCiphertext> = Result.success(GroupCiphertext(nonce = byteArrayOf(1), ciphertext = plaintext))

        override suspend fun decryptMessage(
            ciphertext: GroupCiphertext,
            associatedData: ByteArray,
            groupKey: ByteArray
        ): Result<ByteArray> = Result.success(ciphertext.ciphertext)

        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.success(payload + signingPrivateKey)

        override suspend fun verify(
            payload: ByteArray,
            signature: ByteArray,
            signingPublicKey: ByteArray
        ): Result<Unit> =
            runCatching {
                check(signature.contentEquals(payload + signingPublicKey)) {
                    "Invalid deterministic signature"
                }
            }
    }

    private companion object {
        val CHALLENGE = ByteArray(32) { index -> index.toByte() }
        val OWNER_ENCRYPTION_KEY = byteArrayOf(1)
        val OWNER_SIGNING_KEY = byteArrayOf(2)
        val MEMBER_ENCRYPTION_KEY = byteArrayOf(3)
        val MEMBER_SIGNING_KEY = byteArrayOf(4)
    }
}
