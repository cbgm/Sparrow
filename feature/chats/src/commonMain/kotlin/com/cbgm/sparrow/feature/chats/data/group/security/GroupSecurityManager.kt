package com.cbgm.sparrow.feature.chats.data.group.security

import com.cbgm.sparrow.core.crypto.group.GroupCiphertext
import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.crypto.util.ByteArrays
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupKeyDataSource
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder

class GroupSecurityManager internal constructor(
    private val groupCrypto: GroupCrypto,
    private val cryptoHash: CryptoHash,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupKeyDataSource: GroupKeyDataSource,
    private val groupWelcomeSecurity: GroupWelcomeSecurity
) {
    suspend fun findOwnedGroupEpoch(groupId: String): Result<Int?> =
        runCatching {
            groupSecurityDao.findState(groupId)?.let { state ->
                check(state.localRole.isGroupAdminRole()) {
                    "Only a group admin may change group membership"
                }
                state.currentEpoch
            }
        }

    suspend fun isLocalAdmin(groupId: String): Result<Boolean?> =
        runCatching {
            groupSecurityDao.findState(groupId)?.localRole?.isGroupAdminRole()
        }

    suspend fun findCurrentEpoch(groupId: String): Result<Int?> =
        runCatching { groupSecurityDao.findState(groupId)?.currentEpoch }

    suspend fun findLocalRole(groupId: String): Result<String?> =
        runCatching { groupSecurityDao.findState(groupId)?.localRole }

    suspend fun isLocalMembershipRetired(groupId: String): Result<Boolean> =
        runCatching {
            groupSecurityDao.findState(groupId)?.localRole == GROUP_LEFT_ROLE
        }

    suspend fun findRemoteMemberKey(
        groupId: String,
        contactId: String
    ): Result<GroupMemberKeyEntity?> =
        runCatching {
            val state = groupSecurityDao.findState(groupId) ?: return@runCatching null
            groupSecurityDao.findMemberKey(
                groupId = groupId,
                epoch = state.currentEpoch,
                contactId = contactId
            )
        }

    suspend fun requireRemoteAdmin(
        groupId: String,
        contactId: String,
        signingPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            val state = groupSecurityDao.findState(groupId)
                ?: error("Group security state was not found")
            val memberKey =
                groupSecurityDao.findMemberKey(
                    groupId = groupId,
                    epoch = state.currentEpoch,
                    contactId = contactId
                ) ?: error("Group authority is not part of the current epoch")
            check(memberKey.role.isGroupAdminRole()) {
                "Group update sender is not an admin"
            }
            check(memberKey.signingPublicKey.contentEquals(signingPublicKey)) {
                "Group admin signing identity changed"
            }
        }

    suspend fun isOwnedGroup(groupId: String): Result<Boolean?> = isLocalAdmin(groupId)

    suspend fun deleteLocalGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            groupKeyDataSource.deleteGroup(groupId).getOrThrow()
            groupSecurityDao.deleteGroup(groupId)
        }

    suspend fun retireLocalMembership(
        groupId: String,
        retiredAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            require(retiredAtEpochMilliseconds >= 0L) { "Retirement timestamp must not be negative" }
            groupKeyDataSource.deleteGroup(groupId).getOrThrow()
            val state = groupSecurityDao.findState(groupId) ?: return@runCatching
            check(
                groupSecurityDao.updateLocalRole(
                    groupId = groupId,
                    role = GROUP_LEFT_ROLE,
                    updatedAtEpochMilliseconds = maxOf(state.updatedAtEpochMilliseconds, retiredAtEpochMilliseconds)
                ) == 1
            ) { "Group security state disappeared while local membership was retired" }
        }

    suspend fun clearRetiredMembershipBeforeRejoin(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val state = groupSecurityDao.findState(groupId) ?: return@runCatching
            if (state.localRole != GROUP_LEFT_ROLE) return@runCatching
            groupKeyDataSource.deleteGroup(groupId).getOrThrow()
            groupSecurityDao.deleteGroup(groupId)
        }

    suspend fun removeLocalMembership(
        packet: GroupMemberRemovedPacket,
        ownerContactId: String,
        localSigningPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            val state = groupSecurityDao.findState(packet.groupId)
            if (packet.epoch > GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                state?.let { activeState ->
                    val authority =
                        groupSecurityDao.findMemberKey(
                            groupId = packet.groupId,
                            epoch = activeState.currentEpoch,
                            contactId = ownerContactId
                        ) ?: error("Group removal sender is not part of the current epoch")
                    check(authority.role.isGroupAdminRole()) {
                        "Group removal came from a contact that is not an admin"
                    }
                    check(packet.epoch > activeState.currentEpoch) {
                        "Group removal must reference a later epoch"
                    }
                    check(activeState.localSigningPublicKey.contentEquals(localSigningPublicKey)) {
                        "Local group identity does not match the current security state"
                    }
                }
                check(packet.removedMemberSigningPublicKey.contentEquals(localSigningPublicKey)) {
                    "Group removal targets a different member"
                }
            }

            groupKeyDataSource.deleteGroup(packet.groupId).getOrThrow()
            groupSecurityDao.deleteGroup(packet.groupId)
        }

    suspend fun createOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipientDto>,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<CreatedGroupSecurityDto> =
        runCatching {
            val existingState = groupSecurityDao.findState(groupId)
            val state =
                if (existingState == null) {
                    GroupSecurityStateEntity(
                        groupId = groupId,
                        currentEpoch = INITIAL_EPOCH,
                        welcomePacketId = null,
                        ownerContactId = null,
                        ownerSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                        localSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                        localRole = GROUP_OWNER_ROLE,
                        updatedAtEpochMilliseconds = createdAtEpochMilliseconds
                    )
                } else {
                    check(
                        existingState.currentEpoch == INITIAL_EPOCH &&
                            existingState.welcomePacketId == null &&
                            existingState.localRole.isGroupAdminRole() &&
                            existingState.ownerSigningPublicKey.contentEquals(localSigningKeyPair.publicKey) &&
                            existingState.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)
                    ) {
                        "Existing group security state does not belong to this owner activation"
                    }
                    existingState
                }
            val groupKey =
                if (existingState == null) {
                    groupCrypto.generateGroupKey().getOrThrow().also { generatedKey ->
                        groupKeyDataSource
                            .save(
                                groupId = groupId,
                                epoch = INITIAL_EPOCH,
                                groupKey = generatedKey
                            ).getOrThrow()
                    }
                } else {
                    groupKeyDataSource
                        .load(groupId, INITIAL_EPOCH)
                        .getOrThrow()
                        ?: error("Existing owner group key was not found")
                }

            if (existingState == null) {
                groupSecurityDao.replaceCurrentEpoch(state = state, memberKeys = memberKeys)
            } else {
                groupSecurityDao.upsertMemberKeys(memberKeys)
            }

            val packets =
                recipients.associate { recipient ->
                    val wrappedGroupKey =
                        groupCrypto
                            .wrapGroupKey(
                                groupKey = groupKey,
                                recipientEncryptionPublicKey = recipient.encryptionPublicKey
                            ).getOrThrow()
                    val unsignedPacket =
                        GroupCreatedPacket(
                            packetId = welcomePacketId(groupId, recipient.invitationId, INITIAL_EPOCH),
                            groupId = groupId,
                            title = title,
                            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                            epoch = INITIAL_EPOCH,
                            members = memberPayloads,
                            wrappedGroupKey = wrappedGroupKey,
                            ownerSignature = UNSIGNED_PACKET_MARKER
                        )
                    val signature =
                        groupCrypto
                            .sign(
                                payload = payloadEncoder.encodeWelcome(unsignedPacket),
                                signingPrivateKey = localSigningKeyPair.privateKey
                            ).getOrThrow()

                    recipient.contactId to unsignedPacket.copy(ownerSignature = signature)
                }

            CreatedGroupSecurityDto(welcomePacketsByContactId = packets)
        }

    suspend fun rotateOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipientDto>,
        localSigningKeyPair: LocalSigningKeyPair,
        membershipChange: GroupMembershipChangePayload? = null
    ): Result<CreatedGroupSecurityDto> =
        runCatching {
            val existingState =
                groupSecurityDao.findState(groupId)
                    ?: error("Group security state was not found")
            check(existingState.localRole.isGroupAdminRole()) {
                "Only a group admin may rotate the group epoch"
            }
            check(existingState.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local admin signing key does not match the current security state"
            }

            val nextEpoch = existingState.currentEpoch + 1
            check(memberKeys.all { memberKey -> memberKey.epoch == nextEpoch }) {
                "Every member key must belong to the next group epoch"
            }
            val groupKey = groupCrypto.generateGroupKey().getOrThrow()
            groupKeyDataSource
                .save(
                    groupId = groupId,
                    epoch = nextEpoch,
                    groupKey = groupKey
                ).getOrThrow()

            val nextState =
                existingState.copy(
                    currentEpoch = nextEpoch,
                    welcomePacketId = null,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            groupSecurityDao.replaceCurrentEpoch(
                state = nextState,
                memberKeys = memberKeys
            )

            val packets =
                recipients.associate { recipient ->
                    val wrappedGroupKey =
                        groupCrypto
                            .wrapGroupKey(
                                groupKey = groupKey,
                                recipientEncryptionPublicKey = recipient.encryptionPublicKey
                            ).getOrThrow()
                    val unsignedPacket =
                        GroupCreatedPacket(
                            packetId = welcomePacketId(groupId, recipient.invitationId, nextEpoch),
                            groupId = groupId,
                            title = title,
                            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                            epoch = nextEpoch,
                            members = memberPayloads,
                            wrappedGroupKey = wrappedGroupKey,
                            ownerSignature = UNSIGNED_PACKET_MARKER,
                            membershipChange = membershipChange
                        )
                    val signature =
                        groupCrypto
                            .sign(
                                payload = payloadEncoder.encodeWelcome(unsignedPacket),
                                signingPrivateKey = localSigningKeyPair.privateKey
                            ).getOrThrow()

                    recipient.contactId to unsignedPacket.copy(ownerSignature = signature)
                }

            groupKeyDataSource
                .deleteBefore(
                    groupId = groupId,
                    epoch = nextEpoch
                ).getOrThrow()

            CreatedGroupSecurityDto(welcomePacketsByContactId = packets)
        }

    fun welcomePacketId(
        groupId: String,
        invitationId: String,
        epoch: Int
    ): String = "group-welcome-$groupId-$invitationId-$epoch"

    fun createKeyConfirmation(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): ByteArray {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(groupKey.isNotEmpty()) { "Group key must not be empty" }

        return cryptoHash.sha256(
            ByteArrays.concatenate(
                GROUP_KEY_CONFIRMATION_DOMAIN,
                ByteArrays.withLengthPrefix(groupKey),
                ByteArrays.withLengthPrefix(groupId.encodeToByteArray()),
                ByteArrays.encodeInt(epoch)
            )
        )
    }

    suspend fun verifyKeyConfirmation(
        groupId: String,
        epoch: Int,
        keyConfirmation: ByteArray
    ): Result<Unit> =
        runCatching {
            val groupKey =
                groupKeyDataSource
                    .load(groupId, epoch)
                    .getOrThrow()
                    ?: error("Group key was not found")
            check(createKeyConfirmation(groupId, epoch, groupKey).contentEquals(keyConfirmation)) {
                "Group key confirmation does not match"
            }
        }

    suspend fun openWelcome(
        packet: GroupCreatedPacket,
        senderContactId: String,
        expectedOwnerEncryptionPublicKey: ByteArray,
        expectedOwnerSigningPublicKey: ByteArray,
        localEncryptionKeyPair: LocalEncryptionKeyPair,
        localSigningPublicKey: ByteArray
    ): Result<OpenedGroupWelcomeDto> =
        groupWelcomeSecurity.openWelcome(
            packet = packet,
            senderContactId = senderContactId,
            expectedOwnerEncryptionPublicKey = expectedOwnerEncryptionPublicKey,
            expectedOwnerSigningPublicKey = expectedOwnerSigningPublicKey,
            localEncryptionKeyPair = localEncryptionKeyPair,
            localSigningPublicKey = localSigningPublicKey
        )

    suspend fun persistJoinedGroup(
        openedWelcome: OpenedGroupWelcomeDto,
        ownerContactId: String,
        authoritySigningPublicKey: ByteArray,
        localSigningPublicKey: ByteArray,
        memberKeys: List<GroupMemberKeyEntity>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        groupWelcomeSecurity.persistJoinedGroup(
            openedWelcome = openedWelcome,
            ownerContactId = ownerContactId,
            authoritySigningPublicKey = authoritySigningPublicKey,
            localSigningPublicKey = localSigningPublicKey,
            memberKeys = memberKeys,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    suspend fun encryptMessage(
        groupId: String,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair,
        profilePicture: ProfilePictureMetadata = ProfilePictureMetadata()
    ): Result<SecuredGroupMessageDto> =
        runCatching {
            val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
            check(state.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local signing identity is not a member of the current group epoch"
            }
            val groupKey =
                groupKeyDataSource
                    .load(groupId, state.currentEpoch)
                    .getOrThrow()
                    ?: error("Group key was not found")
            val associatedData =
                payloadEncoder.encodeMessageAssociatedData(
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    epoch = state.currentEpoch,
                    messageId = messageId,
                    sentAtEpochMilliseconds = sentAtEpochMilliseconds,
                    profilePicture = profilePicture
                )
            val encrypted =
                groupCrypto
                    .encryptMessage(
                        plaintext = plaintext.encodeToByteArray(),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
            val signaturePayload =
                payloadEncoder.encodeMessageSignature(
                    associatedData = associatedData,
                    nonce = encrypted.nonce,
                    ciphertext = encrypted.ciphertext
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = signaturePayload,
                        signingPrivateKey = localSigningKeyPair.privateKey
                    ).getOrThrow()

            SecuredGroupMessageDto(
                epoch = state.currentEpoch,
                nonce = encrypted.nonce,
                ciphertext = encrypted.ciphertext,
                senderSignature = signature
            )
        }

    suspend fun decryptMessage(
        packet: GroupChatMessagePacket,
        senderContactId: String
    ): Result<String> =
        runCatching {
            val state =
                groupSecurityDao.findState(packet.groupId)
                    ?: error("Group security state was not found")
            check(packet.epoch == state.currentEpoch) {
                "Group message uses epoch ${packet.epoch}, expected ${state.currentEpoch}"
            }
            val memberKey =
                groupSecurityDao.findMemberKey(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    contactId = senderContactId
                ) ?: error("Sender is not a member of the current group epoch")
            val associatedData =
                payloadEncoder.encodeMessageAssociatedData(
                    version = packet.version,
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    messageId = packet.messageId,
                    sentAtEpochMilliseconds = packet.sentAtEpochMilliseconds,
                    profilePicture = packet.profilePicture
                )
            val signaturePayload =
                payloadEncoder.encodeMessageSignature(
                    associatedData = associatedData,
                    nonce = packet.nonce,
                    ciphertext = packet.ciphertext
                )

            groupCrypto
                .verify(
                    payload = signaturePayload,
                    signature = packet.senderSignature,
                    signingPublicKey = memberKey.signingPublicKey
                ).getOrThrow()

            val groupKey =
                groupKeyDataSource
                    .load(packet.groupId, packet.epoch)
                    .getOrThrow()
                    ?: error("Group key was not found")
            val plaintext =
                groupCrypto
                    .decryptMessage(
                        ciphertext =
                            GroupCiphertext(
                                nonce = packet.nonce,
                                ciphertext = packet.ciphertext
                            ),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
                    .decodeToString(throwOnInvalidSequence = true)

            require(plaintext.isNotBlank()) { "Decrypted group message must not be blank" }
            plaintext
        }

    private companion object {
        const val INITIAL_EPOCH = 1
        val GROUP_KEY_CONFIRMATION_DOMAIN = "sparrow.group-key-confirmation.v1".encodeToByteArray()
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
