package com.cbgm.sparrow.feature.membership.domain.repository

import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.OpenedGroupWelcomeDto
import com.cbgm.sparrow.feature.membership.domain.model.SecuredGroupMessageDto

/** Membership owns the secure group epoch/key lifecycle and message crypto. */
interface GroupSecurityRepository {
    /** Install the owner-only first epoch before any invitation is accepted. Idempotent. */
    suspend fun initializeOwnedGroup(
        groupId: String,
        createdAtEpochMilliseconds: Long,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<Unit>

    fun createKeyConfirmation(groupId: String, epoch: Int, groupKey: ByteArray): ByteArray

    suspend fun openWelcome(
        packet: GroupCreatedPacket,
        senderContactId: String,
        expectedOwnerEncryptionPublicKey: ByteArray,
        expectedOwnerSigningPublicKey: ByteArray,
        localEncryptionKeyPair: LocalEncryptionKeyPair,
        localSigningPublicKey: ByteArray
    ): Result<OpenedGroupWelcomeDto>

    suspend fun persistJoinedGroup(
        openedWelcome: OpenedGroupWelcomeDto,
        ownerContactId: String,
        authoritySigningPublicKey: ByteArray,
        localSigningPublicKey: ByteArray,
        memberKeys: List<GroupWelcomeMemberKey>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun encryptMessage(
        groupId: String,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair,
        profilePicture: ProfilePictureMetadata = ProfilePictureMetadata()
    ): Result<SecuredGroupMessageDto>

    suspend fun encryptMessageDeletion(
        groupId: String,
        deletionId: String,
        deletedAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<SecuredGroupMessageDto>

    suspend fun encryptMessageEdit(
        groupId: String,
        editId: String,
        editedAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<SecuredGroupMessageDto>

    suspend fun decryptMessage(packet: GroupChatMessagePacket, senderContactId: String): Result<String>

    suspend fun decryptMessageDeletion(packet: GroupMessageDeletionPacket, senderContactId: String): Result<String>

    suspend fun decryptMessageEdit(packet: GroupMessageEditPacket, senderContactId: String): Result<String>
}
