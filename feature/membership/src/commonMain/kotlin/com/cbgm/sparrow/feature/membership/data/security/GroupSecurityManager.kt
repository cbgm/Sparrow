package com.cbgm.sparrow.feature.membership.data.security

import com.cbgm.sparrow.core.crypto.group.GroupCiphertext
import com.cbgm.sparrow.core.crypto.group.GroupCrypto
import com.cbgm.sparrow.core.crypto.group.GroupKeyConfirmation
import com.cbgm.sparrow.core.crypto.group.GroupKeyStore
import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.GroupProtocolPayloadEncoder
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.version.ProtocolVersion
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import com.cbgm.sparrow.feature.membership.data.datasource.GroupSecurityStoreDataSource
import com.cbgm.sparrow.feature.membership.data.model.CreatedGroupSecurityDto
import com.cbgm.sparrow.feature.membership.data.model.GROUP_LEFT_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GROUP_OWNER_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupWelcomeRecipientDto
import com.cbgm.sparrow.feature.membership.data.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.OpenedGroupWelcomeDto
import com.cbgm.sparrow.feature.membership.domain.model.SecuredGroupMessageDto
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository

internal class GroupSecurityManager internal constructor(
    private val groupCrypto: GroupCrypto,
    private val cryptoHash: CryptoHash,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val groupSecurityStore: GroupSecurityStoreDataSource,
    private val groupKeyDataSource: GroupKeyStore,
    private val groupWelcomeSecurity: GroupWelcomeSecurity
) : GroupSecurityRepository {
    suspend fun deleteLocalGroup(groupId: String): Result<Unit> = safeSuspendCall {
        groupSecurityStore.deleteGroup(groupId)
        groupKeyDataSource.deleteGroup(groupId)
    }

    suspend fun retireLocalMembership(groupId: String, retiredAtEpochMilliseconds: Long): Result<Unit> = safeSuspendCall {
        groupSecurityStore.retireLocalMembership(groupId, retiredAtEpochMilliseconds)
        groupKeyDataSource.deleteGroup(groupId)
    }

    suspend fun clearRetiredMembershipBeforeRejoin(groupId: String): Result<Unit> = safeSuspendCall {
        groupSecurityStore.findState(groupId)?.let { state ->
            check(state.localRole == GROUP_LEFT_ROLE) { "Local membership is not retired" }
            groupSecurityStore.deleteGroup(groupId)
        }
    }

    suspend fun findOwnedGroupEpoch(groupId: String): Result<Int?> =
        safeSuspendCall {
            groupSecurityStore.findState(groupId)?.let { state ->
                check(state.localRole.isGroupAdminRole()) {
                    "Only a group admin may change group membership"
                }
                state.currentEpoch
            }
        }

    suspend fun isLocalAdmin(groupId: String): Result<Boolean?> =
        safeSuspendCall {
            groupSecurityStore.findState(groupId)?.localRole?.isGroupAdminRole()
        }

    suspend fun findCurrentEpoch(groupId: String): Result<Int?> =
        safeSuspendCall { groupSecurityStore.findState(groupId)?.currentEpoch }

    suspend fun findLocalRole(groupId: String): Result<String?> =
        safeSuspendCall { groupSecurityStore.findState(groupId)?.localRole }

    suspend fun isLocalMembershipRetired(groupId: String): Result<Boolean> =
        safeSuspendCall {
            groupSecurityStore.findState(groupId)?.localRole == GROUP_LEFT_ROLE
        }

    suspend fun findRemoteMemberKey(
        groupId: String,
        contactId: String
    ): Result<GroupMemberKeyEntity?> =
        safeSuspendCall {
            val state = groupSecurityStore.findState(groupId) ?: return@safeSuspendCall null
            groupSecurityStore.findMemberKey(
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
        safeSuspendCall {
            val state = groupSecurityStore.findState(groupId)
                ?: error("Group security state was not found")
            val memberKey =
                groupSecurityStore.findMemberKey(
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

    suspend fun createOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipientDto>,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<CreatedGroupSecurityDto> =
        safeSuspendCall {
            val existingState = groupSecurityStore.findState(groupId)
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
                            )
                    }
                } else {
                    groupKeyDataSource
                        .load(groupId, INITIAL_EPOCH)
                        ?: error("Existing owner group key was not found")
                }

            if (existingState == null) {
                groupSecurityStore.replaceCurrentEpoch(state = state, memberKeys = memberKeys)
            } else {
                groupSecurityStore.upsertMemberKeys(memberKeys)
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
        membershipChange: GroupMembershipChangePayload?
    ): Result<CreatedGroupSecurityDto> =
        safeSuspendCall {
            val existingState =
                groupSecurityStore.findState(groupId)
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
                )

            val nextState =
                existingState.copy(
                    currentEpoch = nextEpoch,
                    welcomePacketId = null,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            groupSecurityStore.replaceCurrentEpoch(
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
                )

            CreatedGroupSecurityDto(welcomePacketsByContactId = packets)
        }

    fun welcomePacketId(
        groupId: String,
        invitationId: String,
        epoch: Int
    ): String = "group-welcome-$groupId-$invitationId-$epoch"

    override fun createKeyConfirmation(groupId: String, epoch: Int, groupKey: ByteArray): ByteArray =
        GroupKeyConfirmation.create(cryptoHash, groupId, epoch, groupKey)

    override suspend fun openWelcome(
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

    override suspend fun persistJoinedGroup(
        openedWelcome: OpenedGroupWelcomeDto,
        ownerContactId: String,
        authoritySigningPublicKey: ByteArray,
        localSigningPublicKey: ByteArray,
        memberKeys: List<GroupWelcomeMemberKey>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        groupWelcomeSecurity.persistJoinedGroup(
            openedWelcome = openedWelcome,
            ownerContactId = ownerContactId,
            authoritySigningPublicKey = authoritySigningPublicKey,
            localSigningPublicKey = localSigningPublicKey,
            memberKeys = memberKeys.map { key ->
                GroupMemberKeyEntity(
                    groupId = key.groupId,
                    epoch = key.epoch,
                    contactId = key.contactId,
                    encryptionPublicKey = key.encryptionPublicKey,
                    signingPublicKey = key.signingPublicKey,
                    role = key.role
                )
            },
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    override suspend fun encryptMessage(
        groupId: String,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair,
        profilePicture: ProfilePictureMetadata
    ): Result<SecuredGroupMessageDto> =
        safeSuspendCall {
            val state = groupSecurityStore.findState(groupId) ?: error("Group security state was not found")
            check(state.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local signing identity is not a member of the current group epoch"
            }
            val groupKey =
                groupKeyDataSource
                    .load(groupId, state.currentEpoch)
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

    override suspend fun encryptMessageDeletion(
        groupId: String,
        deletionId: String,
        deletedAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<SecuredGroupMessageDto> =
        safeSuspendCall {
            val state = groupSecurityStore.findState(groupId) ?: error("Group security state was not found")
            check(state.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local signing identity is not a member of the current group epoch"
            }
            val groupKey =
                groupKeyDataSource
                    .load(groupId, state.currentEpoch)
                    ?: error("Group key was not found")
            val associatedData =
                payloadEncoder.encodeMessageDeletionAssociatedData(
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    epoch = state.currentEpoch,
                    deletionId = deletionId,
                    deletedAtEpochMilliseconds = deletedAtEpochMilliseconds
                )
            val encrypted =
                groupCrypto
                    .encryptMessage(
                        plaintext = plaintext.encodeToByteArray(),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
            val signaturePayload =
                payloadEncoder.encodeMessageDeletionSignature(
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

    override suspend fun encryptMessageEdit(
        groupId: String,
        editId: String,
        editedAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<SecuredGroupMessageDto> =
        safeSuspendCall {
            val state = groupSecurityStore.findState(groupId) ?: error("Group security state was not found")
            check(state.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local signing identity is not a member of the current group epoch"
            }
            val groupKey =
                groupKeyDataSource
                    .load(groupId, state.currentEpoch)
                    ?: error("Group key was not found")
            val associatedData =
                payloadEncoder.encodeMessageEditAssociatedData(
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    epoch = state.currentEpoch,
                    editId = editId,
                    editedAtEpochMilliseconds = editedAtEpochMilliseconds
                )
            val encrypted =
                groupCrypto
                    .encryptMessage(
                        plaintext = plaintext.encodeToByteArray(),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
            val signaturePayload =
                payloadEncoder.encodeMessageEditSignature(
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

    override suspend fun decryptMessage(
        packet: GroupChatMessagePacket,
        senderContactId: String
    ): Result<String> =
        safeSuspendCall {
            val state =
                groupSecurityStore.findState(packet.groupId)
                    ?: error("Group security state was not found")
            check(packet.epoch == state.currentEpoch) {
                "Group message uses epoch ${packet.epoch}, expected ${state.currentEpoch}"
            }
            val memberKey =
                groupSecurityStore.findMemberKey(
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

    override suspend fun decryptMessageDeletion(
        packet: GroupMessageDeletionPacket,
        senderContactId: String
    ): Result<String> =
        safeSuspendCall {
            val state =
                groupSecurityStore.findState(packet.groupId)
                    ?: error("Group security state was not found")
            check(packet.epoch == state.currentEpoch) {
                "Group message deletion uses epoch ${packet.epoch}, expected ${state.currentEpoch}"
            }
            val memberKey =
                groupSecurityStore.findMemberKey(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    contactId = senderContactId
                ) ?: error("Sender is not a member of the current group epoch")
            val associatedData =
                payloadEncoder.encodeMessageDeletionAssociatedData(
                    version = packet.version,
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    deletionId = packet.deletionId,
                    deletedAtEpochMilliseconds = packet.deletedAtEpochMilliseconds
                )
            val signaturePayload =
                payloadEncoder.encodeMessageDeletionSignature(
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

            require(plaintext.isNotBlank()) { "Decrypted group message deletion must not be blank" }
            plaintext
        }

    override suspend fun decryptMessageEdit(
        packet: GroupMessageEditPacket,
        senderContactId: String
    ): Result<String> =
        safeSuspendCall {
            val state =
                groupSecurityStore.findState(packet.groupId)
                    ?: error("Group security state was not found")
            check(packet.epoch == state.currentEpoch) {
                "Group message edit uses epoch ${packet.epoch}, expected ${state.currentEpoch}"
            }
            val memberKey =
                groupSecurityStore.findMemberKey(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    contactId = senderContactId
                ) ?: error("Sender is not a member of the current group epoch")
            val associatedData =
                payloadEncoder.encodeMessageEditAssociatedData(
                    version = packet.version,
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    editId = packet.editId,
                    editedAtEpochMilliseconds = packet.editedAtEpochMilliseconds
                )
            val signaturePayload =
                payloadEncoder.encodeMessageEditSignature(
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
                    ?: error("Group key was not found")
            val plaintext =
                groupCrypto
                    .decryptMessage(
                        ciphertext = GroupCiphertext(packet.nonce, packet.ciphertext),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
                    .decodeToString(throwOnInvalidSequence = true)

            require(plaintext.isNotBlank()) { "Decrypted group message edit must not be blank" }
            plaintext
        }

    private companion object {
        const val INITIAL_EPOCH = 1
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
