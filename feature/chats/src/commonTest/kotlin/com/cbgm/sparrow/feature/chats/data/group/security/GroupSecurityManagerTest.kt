package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.core.crypto.group.GroupCiphertext
import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.crypto.hash.DefaultCryptoHash
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupKeyDataSource
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupSecurityManagerTest {
    private val encoder = GroupProtocolPayloadEncoder()
    private val dao = InMemoryGroupSecurityDao()
    private val keyStorage = InMemoryGroupKeyDataSource()
    private val crypto = DeterministicGroupCrypto()
    private val groupWelcomeSecurity =
        GroupWelcomeSecurity(
            groupCrypto = crypto,
            payloadEncoder = encoder,
            groupSecurityDao = dao,
            groupKeyDataSource = keyStorage
        )
    private val manager =
        GroupSecurityManager(
            groupCrypto = crypto,
            cryptoHash = DefaultCryptoHash(),
            payloadEncoder = encoder,
            groupSecurityDao = dao,
            groupKeyDataSource = keyStorage,
            groupWelcomeSecurity = groupWelcomeSecurity
        )

    @Test
    fun localGroupDeletionClearsSecurityStateAndKeys() =
        runTest {
            manager
                .createOwnedGroup(
                    groupId = GROUP_ID,
                    title = "Group",
                    createdAtEpochMilliseconds = 100L,
                    memberPayloads = emptyList(),
                    memberKeys = emptyList(),
                    recipients = emptyList(),
                    localSigningKeyPair =
                        LocalSigningKeyPair(
                            publicKey = LOCAL_SIGNING_KEY,
                            privateKey = LOCAL_SIGNING_KEY
                        )
                ).getOrThrow()

            assertEquals(true, manager.isOwnedGroup(GROUP_ID).getOrThrow())
            manager.deleteLocalGroup(GROUP_ID).getOrThrow()

            assertNull(manager.isOwnedGroup(GROUP_ID).getOrThrow())
            assertNull(keyStorage.load(GROUP_ID, EPOCH).getOrThrow())
        }

    @Test
    fun retiringMembershipDeletesContentKeyButKeepsRoutingKeysUntilRejoin() =
        runTest {
            val memberKey =
                GroupMemberKeyEntity(
                    groupId = GROUP_ID,
                    epoch = EPOCH,
                    contactId = REMOTE_CONTACT_ID,
                    encryptionPublicKey = byteArrayOf(5),
                    signingPublicKey = REMOTE_SIGNING_KEY,
                    role = GROUP_ADMIN_ROLE
                )
            manager
                .createOwnedGroup(
                    groupId = GROUP_ID,
                    title = "Group",
                    createdAtEpochMilliseconds = 100L,
                    memberPayloads = emptyList(),
                    memberKeys = listOf(memberKey),
                    recipients = emptyList(),
                    localSigningKeyPair =
                        LocalSigningKeyPair(
                            publicKey = LOCAL_SIGNING_KEY,
                            privateKey = LOCAL_SIGNING_KEY
                        )
                ).getOrThrow()

            manager.retireLocalMembership(GROUP_ID, 200L).getOrThrow()

            assertFalse(manager.isOwnedGroup(GROUP_ID).getOrThrow() ?: true)
            assertNull(keyStorage.load(GROUP_ID, EPOCH).getOrThrow())
            assertEquals(GROUP_LEFT_ROLE, dao.findState(GROUP_ID)?.localRole)
            assertContentEquals(
                REMOTE_SIGNING_KEY,
                dao.findMemberKey(GROUP_ID, EPOCH, REMOTE_CONTACT_ID)?.signingPublicKey
            )

            manager.clearRetiredMembershipBeforeRejoin(GROUP_ID).getOrThrow()

            assertNull(dao.findState(GROUP_ID))
            assertNull(dao.findMemberKey(GROUP_ID, EPOCH, REMOTE_CONTACT_ID))
        }

    @Test
    fun ownerActivationIsRetrySafe() =
        runTest {
            val memberKey =
                GroupMemberKeyEntity(
                    groupId = GROUP_ID,
                    epoch = EPOCH,
                    contactId = REMOTE_CONTACT_ID,
                    encryptionPublicKey = byteArrayOf(5),
                    signingPublicKey = REMOTE_SIGNING_KEY,
                    role = "MEMBER"
                )
            val memberPayloads =
                listOf(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = byteArrayOf(8),
                        signingPublicKey = LOCAL_SIGNING_KEY,
                        role = "OWNER",
                        phoneNumber = "+491"
                    ),
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = byteArrayOf(5),
                        signingPublicKey = REMOTE_SIGNING_KEY,
                        role = "MEMBER",
                        phoneNumber = "+492"
                    )
                )
            val signingKeyPair =
                LocalSigningKeyPair(
                    publicKey = LOCAL_SIGNING_KEY,
                    privateKey = LOCAL_SIGNING_KEY
                )

            val first =
                manager
                    .createOwnedGroup(
                        groupId = GROUP_ID,
                        title = "Group",
                        createdAtEpochMilliseconds = 100L,
                        memberPayloads = memberPayloads,
                        memberKeys = listOf(memberKey),
                        recipients =
                            listOf(
                                GroupWelcomeRecipient(
                                    contactId = REMOTE_CONTACT_ID,
                                    invitationId = INVITATION_ID,
                                    encryptionPublicKey = byteArrayOf(5)
                                )
                            ),
                        localSigningKeyPair = signingKeyPair
                    ).getOrThrow()
            val second =
                manager
                    .createOwnedGroup(
                        groupId = GROUP_ID,
                        title = "Group",
                        createdAtEpochMilliseconds = 100L,
                        memberPayloads = memberPayloads,
                        memberKeys = listOf(memberKey),
                        recipients =
                            listOf(
                                GroupWelcomeRecipient(
                                    contactId = REMOTE_CONTACT_ID,
                                    invitationId = INVITATION_ID,
                                    encryptionPublicKey = byteArrayOf(5)
                                )
                            ),
                        localSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            assertEquals(1, crypto.generatedGroupKeyCount)
            assertEquals(
                first.welcomePacketsByContactId.getValue(REMOTE_CONTACT_ID).packetId,
                second.welcomePacketsByContactId.getValue(REMOTE_CONTACT_ID).packetId
            )
        }

    @Test
    fun membershipRotationAdvancesEpochAndRewrapsTheGroupKey() =
        runTest {
            val signingKeyPair =
                LocalSigningKeyPair(
                    publicKey = LOCAL_SIGNING_KEY,
                    privateKey = LOCAL_SIGNING_KEY
                )
            val memberPayloads =
                listOf(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = byteArrayOf(8),
                        signingPublicKey = LOCAL_SIGNING_KEY,
                        role = "OWNER",
                        phoneNumber = "+491"
                    ),
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = byteArrayOf(5),
                        signingPublicKey = REMOTE_SIGNING_KEY,
                        role = "MEMBER",
                        phoneNumber = "+492"
                    )
                )
            val recipient =
                GroupWelcomeRecipient(
                    contactId = REMOTE_CONTACT_ID,
                    invitationId = INVITATION_ID,
                    encryptionPublicKey = byteArrayOf(5)
                )

            manager
                .createOwnedGroup(
                    groupId = GROUP_ID,
                    title = "Group",
                    createdAtEpochMilliseconds = 100L,
                    memberPayloads = memberPayloads,
                    memberKeys = listOf(memberKey(epoch = EPOCH)),
                    recipients = listOf(recipient),
                    localSigningKeyPair = signingKeyPair
                ).getOrThrow()
            val rotated =
                manager
                    .rotateOwnedGroup(
                        groupId = GROUP_ID,
                        title = "Group",
                        createdAtEpochMilliseconds = 100L,
                        updatedAtEpochMilliseconds = 200L,
                        memberPayloads = memberPayloads,
                        memberKeys = listOf(memberKey(epoch = EPOCH + 1)),
                        recipients = listOf(recipient),
                        localSigningKeyPair = signingKeyPair,
                        membershipChange =
                            GroupMembershipChangePayload(
                                reason = GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                                memberSigningPublicKey = REMOTE_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals(EPOCH + 1, manager.findOwnedGroupEpoch(GROUP_ID).getOrThrow())
            assertEquals(
                EPOCH + 1,
                rotated.welcomePacketsByContactId.getValue(REMOTE_CONTACT_ID).epoch
            )
            assertEquals(
                GroupMemberRemovedPacket.REASON_MEMBER_LEFT,
                rotated.welcomePacketsByContactId
                    .getValue(REMOTE_CONTACT_ID)
                    .membershipChange
                    ?.reason
            )
            assertEquals(2, crypto.generatedGroupKeyCount)
        }

    @Test
    fun rejoiningMemberCanOpenCurrentEpochWithoutPreviousLocalState() =
        runTest {
            val localEncryptionKey = byteArrayOf(8)
            val unsignedPacket =
                GroupCreatedPacket(
                    packetId = manager.welcomePacketId(GROUP_ID, INVITATION_ID, 3),
                    groupId = GROUP_ID,
                    title = "Group",
                    createdAtEpochMilliseconds = 100L,
                    epoch = 3,
                    members =
                        listOf(
                            GroupMemberPayload(
                                displayName = null,
                                encryptionPublicKey = byteArrayOf(5),
                                signingPublicKey = REMOTE_SIGNING_KEY,
                                role = "OWNER",
                                phoneNumber = "+491"
                            ),
                            GroupMemberPayload(
                                displayName = null,
                                encryptionPublicKey = localEncryptionKey,
                                signingPublicKey = LOCAL_SIGNING_KEY,
                                role = "MEMBER",
                                phoneNumber = "+492"
                            )
                        ),
                    wrappedGroupKey = GROUP_KEY + localEncryptionKey,
                    ownerSignature = byteArrayOf(0)
                )
            val packet =
                unsignedPacket.copy(
                    ownerSignature =
                        encoder.encodeWelcome(unsignedPacket) + REMOTE_SIGNING_KEY
                )

            val opened =
                manager
                    .openWelcome(
                        packet = packet,
                        senderContactId = REMOTE_CONTACT_ID,
                        expectedOwnerEncryptionPublicKey = byteArrayOf(5),
                        expectedOwnerSigningPublicKey = REMOTE_SIGNING_KEY,
                        localEncryptionKeyPair =
                            LocalEncryptionKeyPair(
                                publicKey = localEncryptionKey,
                                privateKey = localEncryptionKey
                            ),
                        localSigningPublicKey = LOCAL_SIGNING_KEY
                    ).getOrThrow()

            assertEquals(3, opened.packet.epoch)
            assertContentEquals(GROUP_KEY, opened.groupKey)
        }

    @Test
    fun epochRemovalIsAcceptedWhenWelcomeHasNotBeenInstalledYet() =
        runTest {
            seedSecurityState()

            manager
                .removeLocalMembership(
                    packet =
                        GroupMemberRemovedPacket(
                            packetId = "removal-packet",
                            invitationId = INVITATION_ID,
                            groupId = GROUP_ID,
                            epoch = 3,
                            challenge = byteArrayOf(1),
                            removedMemberSigningPublicKey = LOCAL_SIGNING_KEY,
                            removedAtEpochMilliseconds = 300L,
                            ownerSignature = byteArrayOf(2)
                        ),
                    ownerContactId = REMOTE_CONTACT_ID,
                    localSigningPublicKey = LOCAL_SIGNING_KEY
                ).getOrThrow()

            assertEquals(null, keyStorage.load(GROUP_ID, EPOCH).getOrThrow())
            assertEquals(null, dao.findState(GROUP_ID))
        }

    @Test
    fun encryptsAndSignsWithCurrentEpochAndDecryptsForStoredMember() =
        runTest {
            seedSecurityState()

            val secured =
                manager
                    .encryptMessage(
                        groupId = GROUP_ID,
                        messageId = "local-message",
                        sentAtEpochMilliseconds = 123L,
                        plaintext = "local hello",
                        localSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = LOCAL_SIGNING_KEY,
                                privateKey = LOCAL_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals(EPOCH, secured.epoch)
            assertContentEquals("local hello".encodeToByteArray().reversedArray(), secured.ciphertext)

            val incoming = createRemotePacket(text = "remote hello")
            assertEquals(
                "remote hello",
                manager.decryptMessage(incoming, REMOTE_CONTACT_ID).getOrThrow()
            )
        }

    @Test
    fun rejectsTamperedSenderSignature() =
        runTest {
            seedSecurityState()
            val packet = createRemotePacket(text = "authenticated").copy(senderSignature = byteArrayOf(99))

            assertTrue(manager.decryptMessage(packet, REMOTE_CONTACT_ID).isFailure)
        }

    @Test
    fun keyConfirmationProvesPossessionOfTheCurrentEpochKey() =
        runTest {
            seedSecurityState()
            val confirmation =
                manager.createKeyConfirmation(
                    groupId = GROUP_ID,
                    epoch = EPOCH,
                    groupKey = GROUP_KEY
                )

            manager
                .verifyKeyConfirmation(
                    groupId = GROUP_ID,
                    epoch = EPOCH,
                    keyConfirmation = confirmation
                ).getOrThrow()
            assertTrue(
                manager
                    .verifyKeyConfirmation(
                        groupId = GROUP_ID,
                        epoch = EPOCH,
                        keyConfirmation = confirmation.copyOf().also { it[0] = (it[0] + 1).toByte() }
                    ).isFailure
            )
        }

    @Test
    fun promotedAdminCanRotateGroupEpoch() =
        runTest {
            dao.upsertState(
                GroupSecurityStateEntity(
                    groupId = GROUP_ID,
                    currentEpoch = EPOCH,
                    welcomePacketId = "welcome-1",
                    ownerContactId = REMOTE_CONTACT_ID,
                    ownerSigningPublicKey = REMOTE_SIGNING_KEY,
                    localSigningPublicKey = LOCAL_SIGNING_KEY,
                    localRole = GROUP_ADMIN_ROLE,
                    updatedAtEpochMilliseconds = 100L
                )
            )
            dao.upsertMemberKeys(listOf(memberKey(epoch = EPOCH)))
            keyStorage.save(GROUP_ID, EPOCH, GROUP_KEY).getOrThrow()

            val nextEpoch = EPOCH + 1
            manager
                .rotateOwnedGroup(
                    groupId = GROUP_ID,
                    title = "Group",
                    createdAtEpochMilliseconds = 100L,
                    updatedAtEpochMilliseconds = 200L,
                    memberPayloads =
                        listOf(
                            GroupMemberPayload(
                                displayName = null,
                                encryptionPublicKey = byteArrayOf(8),
                                signingPublicKey = LOCAL_SIGNING_KEY,
                                role = GROUP_ADMIN_ROLE,
                                phoneNumber = "+491"
                            ),
                            GroupMemberPayload(
                                displayName = null,
                                encryptionPublicKey = byteArrayOf(5),
                                signingPublicKey = REMOTE_SIGNING_KEY,
                                role = GROUP_MEMBER_ROLE,
                                phoneNumber = "+492"
                            )
                        ),
                    memberKeys = listOf(memberKey(epoch = nextEpoch)),
                    recipients =
                        listOf(
                            GroupWelcomeRecipient(
                                contactId = REMOTE_CONTACT_ID,
                                invitationId = INVITATION_ID,
                                encryptionPublicKey = byteArrayOf(5)
                            )
                        ),
                    localSigningKeyPair =
                        LocalSigningKeyPair(
                            publicKey = LOCAL_SIGNING_KEY,
                            privateKey = LOCAL_SIGNING_KEY
                        )
                ).getOrThrow()

            assertEquals(nextEpoch, manager.findOwnedGroupEpoch(GROUP_ID).getOrThrow())
        }

    @Test
    fun normalMemberCannotRotateGroupEpoch() =
        runTest {
            seedSecurityState()

            val result =
                manager.rotateOwnedGroup(
                    groupId = GROUP_ID,
                    title = "Group",
                    createdAtEpochMilliseconds = 100L,
                    updatedAtEpochMilliseconds = 200L,
                    memberPayloads = emptyList(),
                    memberKeys = emptyList(),
                    recipients = emptyList(),
                    localSigningKeyPair =
                        LocalSigningKeyPair(
                            publicKey = LOCAL_SIGNING_KEY,
                            privateKey = LOCAL_SIGNING_KEY
                        )
                )

            assertTrue(result.isFailure)
            assertEquals(EPOCH, manager.findCurrentEpoch(GROUP_ID).getOrThrow())
        }

    private suspend fun seedSecurityState() {
        dao.upsertState(
            GroupSecurityStateEntity(
                groupId = GROUP_ID,
                currentEpoch = EPOCH,
                welcomePacketId = "welcome-1",
                ownerContactId = REMOTE_CONTACT_ID,
                ownerSigningPublicKey = REMOTE_SIGNING_KEY,
                localSigningPublicKey = LOCAL_SIGNING_KEY,
                localRole = "MEMBER",
                updatedAtEpochMilliseconds = 100L
            )
        )
        dao.upsertMemberKeys(
            listOf(
                GroupMemberKeyEntity(
                    groupId = GROUP_ID,
                    epoch = EPOCH,
                    contactId = REMOTE_CONTACT_ID,
                    encryptionPublicKey = byteArrayOf(5),
                    signingPublicKey = REMOTE_SIGNING_KEY,
                    role = "OWNER"
                )
            )
        )
        keyStorage.save(GROUP_ID, EPOCH, GROUP_KEY).getOrThrow()
    }

    private fun memberKey(epoch: Int): GroupMemberKeyEntity =
        GroupMemberKeyEntity(
            groupId = GROUP_ID,
            epoch = epoch,
            contactId = REMOTE_CONTACT_ID,
            encryptionPublicKey = byteArrayOf(5),
            signingPublicKey = REMOTE_SIGNING_KEY,
            role = "MEMBER"
        )

    private fun createRemotePacket(text: String): GroupChatMessagePacket {
        val associatedData =
            encoder.encodeMessageAssociatedData(
                version = 1,
                groupId = GROUP_ID,
                epoch = EPOCH,
                messageId = "remote-message",
                sentAtEpochMilliseconds = 456L
            )
        val ciphertext = text.encodeToByteArray().reversedArray()
        val signaturePayload =
            encoder.encodeMessageSignature(
                associatedData = associatedData,
                nonce = NONCE,
                ciphertext = ciphertext
            )

        return GroupChatMessagePacket(
            packetId = "remote-packet",
            groupId = GROUP_ID,
            epoch = EPOCH,
            messageId = "remote-message",
            sentAtEpochMilliseconds = 456L,
            nonce = NONCE,
            ciphertext = ciphertext,
            senderSignature = signaturePayload + REMOTE_SIGNING_KEY
        )
    }

    private class DeterministicGroupCrypto : GroupCrypto {
        var generatedGroupKeyCount = 0

        override suspend fun generateGroupKey(): Result<ByteArray> {
            generatedGroupKeyCount += 1
            return Result.success(GROUP_KEY)
        }

        override suspend fun generateInvitationChallenge(): Result<ByteArray> = Result.success(ByteArray(32) { index -> index.toByte() })

        override suspend fun wrapGroupKey(
            groupKey: ByteArray,
            recipientEncryptionPublicKey: ByteArray
        ): Result<ByteArray> = Result.success(groupKey + recipientEncryptionPublicKey)

        override suspend fun unwrapGroupKey(
            wrappedGroupKey: ByteArray,
            localEncryptionPublicKey: ByteArray,
            localEncryptionPrivateKey: ByteArray
        ): Result<ByteArray> = Result.success(wrappedGroupKey.copyOf(GROUP_KEY.size))

        override suspend fun encryptMessage(
            plaintext: ByteArray,
            associatedData: ByteArray,
            groupKey: ByteArray
        ): Result<GroupCiphertext> =
            Result.success(
                GroupCiphertext(
                    nonce = NONCE,
                    ciphertext = plaintext.reversedArray()
                )
            )

        override suspend fun decryptMessage(
            ciphertext: GroupCiphertext,
            associatedData: ByteArray,
            groupKey: ByteArray
        ): Result<ByteArray> = Result.success(ciphertext.ciphertext.reversedArray())

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

    private class InMemoryGroupKeyDataSource : GroupKeyDataSource {
        private val values = mutableMapOf<Pair<String, Int>, ByteArray>()

        override suspend fun save(
            groupId: String,
            epoch: Int,
            groupKey: ByteArray
        ): Result<Unit> =
            runCatching {
                values[groupId to epoch] = groupKey.copyOf()
            }

        override suspend fun load(
            groupId: String,
            epoch: Int
        ): Result<ByteArray?> = Result.success(values[groupId to epoch]?.copyOf())

        override suspend fun deleteBefore(
            groupId: String,
            epoch: Int
        ): Result<Unit> =
            runCatching {
                values.keys
                    .filter { (storedGroupId, storedEpoch) ->
                        storedGroupId == groupId && storedEpoch < epoch
                    }.forEach { key -> values.remove(key) }
            }

        override suspend fun deleteGroup(groupId: String): Result<Unit> =
            runCatching {
                values.keys
                    .filter { (storedGroupId, _) -> storedGroupId == groupId }
                    .forEach { key -> values.remove(key) }
            }
    }

    private class InMemoryGroupSecurityDao : GroupSecurityDao {
        private val state = MutableStateFlow<GroupSecurityStateEntity?>(null)
        private val memberKeys = mutableListOf<GroupMemberKeyEntity>()

        override suspend fun upsertState(state: GroupSecurityStateEntity) {
            this.state.value = state
        }

        override suspend fun upsertMemberKeys(memberKeys: List<GroupMemberKeyEntity>) {
            memberKeys.forEach { incoming ->
                this.memberKeys.removeAll { existing ->
                    existing.groupId == incoming.groupId &&
                        existing.epoch == incoming.epoch &&
                        existing.contactId == incoming.contactId
                }
                this.memberKeys += incoming
            }
        }

        override suspend fun findState(groupId: String): GroupSecurityStateEntity? = state.value?.takeIf { storedState -> storedState.groupId == groupId }

        override fun observeState(groupId: String): Flow<GroupSecurityStateEntity?> =
            state.map { storedState ->
                storedState?.takeIf { it.groupId == groupId }
            }

        override suspend fun deleteState(groupId: String) {
            if (state.value?.groupId == groupId) {
                state.value = null
            }
        }

        override suspend fun deleteMemberKeys(groupId: String) {
            memberKeys.removeAll { member -> member.groupId == groupId }
        }

        override suspend fun findMemberKey(
            groupId: String,
            epoch: Int,
            contactId: String
        ): GroupMemberKeyEntity? =
            memberKeys.firstOrNull { member ->
                member.groupId == groupId &&
                    member.epoch == epoch &&
                    member.contactId == contactId
            }

        override suspend fun findLatestMemberKey(
            groupId: String,
            contactId: String
        ): GroupMemberKeyEntity? =
            memberKeys
                .filter { member -> member.groupId == groupId && member.contactId == contactId }
                .maxByOrNull(GroupMemberKeyEntity::epoch)

        override suspend fun findMemberKeys(
            groupId: String,
            epoch: Int
        ): List<GroupMemberKeyEntity> =
            memberKeys.filter { member -> member.groupId == groupId && member.epoch == epoch }

        override fun observeCurrentMemberKeys(groupId: String): Flow<List<GroupMemberKeyEntity>> =
            state.map { currentState ->
                val epoch = currentState?.takeIf { it.groupId == groupId }?.currentEpoch
                if (epoch == null) {
                    emptyList()
                } else {
                    memberKeys.filter { member -> member.groupId == groupId && member.epoch == epoch }
                }
            }

        override suspend fun findAllCurrentMemberKeys(): List<GroupMemberKeyEntity> {
            val currentState = state.value ?: return emptyList()
            return memberKeys.filter { member ->
                member.groupId == currentState.groupId &&
                    member.epoch == currentState.currentEpoch
            }
        }

        override suspend fun updateLocalRole(
            groupId: String,
            role: String,
            updatedAtEpochMilliseconds: Long
        ): Int {
            val current = state.value ?: return 0
            if (current.groupId != groupId) return 0
            state.value = current.copy(localRole = role, updatedAtEpochMilliseconds = updatedAtEpochMilliseconds)
            return 1
        }
    }

    private companion object {
        const val GROUP_ID = "group-1"
        const val INVITATION_ID = "invitation-1"
        const val EPOCH = 1
        const val REMOTE_CONTACT_ID = "contact-remote"
        val GROUP_KEY = ByteArray(32) { index -> index.toByte() }
        val NONCE = byteArrayOf(1, 2, 3)
        val LOCAL_SIGNING_KEY = byteArrayOf(4)
        val REMOTE_SIGNING_KEY = byteArrayOf(6)
    }
}
