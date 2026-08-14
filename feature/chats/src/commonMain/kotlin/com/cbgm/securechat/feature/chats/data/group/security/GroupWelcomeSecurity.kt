package com.cbgm.securechat.feature.chats.data.group.security

import com.cbgm.securechat.core.crypto.group.GroupCrypto
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupSecurityStateEntity
import com.cbgm.securechat.feature.chats.data.group.protocol.GroupProtocolPayloadEncoder
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupKeyRepository

internal class GroupWelcomeSecurity(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupKeyRepository: GroupKeyRepository
) {
    suspend fun openWelcome(
        packet: GroupCreatedPacket,
        senderContactId: String,
        expectedOwnerEncryptionPublicKey: ByteArray,
        expectedOwnerSigningPublicKey: ByteArray,
        localEncryptionKeyPair: LocalEncryptionKeyPair,
        localSigningPublicKey: ByteArray
    ): Result<OpenedGroupWelcome> =
        runCatching {
            val existingState = groupSecurityDao.findState(packet.groupId)
            if (existingState == null) {
                check(packet.epoch >= INITIAL_EPOCH) {
                    "Group welcome epoch must be at least $INITIAL_EPOCH"
                }
            } else {
                check(
                    packet.epoch == existingState.currentEpoch ||
                        packet.epoch == existingState.currentEpoch + 1
                ) {
                    "Group welcome epoch must repeat the current epoch or advance it by one"
                }
                val authority =
                    groupSecurityDao.findMemberKey(
                        groupId = packet.groupId,
                        epoch = existingState.currentEpoch,
                        contactId = senderContactId
                    ) ?: error("Group update sender is not part of the current epoch")
                check(authority.role.isGroupAdminRole()) {
                    "Group update sender is not an admin"
                }
                check(authority.signingPublicKey.contentEquals(expectedOwnerSigningPublicKey)) {
                    "Group admin signing key changed during the epoch update"
                }
                check(existingState.localSigningPublicKey.contentEquals(localSigningPublicKey)) {
                    "Local signing identity changed during the epoch update"
                }
            }
            check(
                packet.members.all { member ->
                    member.encryptionPublicKey.isNotEmpty() && member.signingPublicKey.isNotEmpty()
                }
            ) {
                "Secure group members must all have public keys"
            }

            check(packet.members.any { member -> member.role.isGroupAdminRole() }) {
                "Group welcome must contain at least one admin"
            }
            if (existingState == null) {
                val authority =
                    packet.members.singleOrNull { member ->
                        member.signingPublicKey.contentEquals(expectedOwnerSigningPublicKey)
                    } ?: error("Authenticated group admin is missing from the group welcome")
                check(authority.role.isGroupAdminRole()) {
                    "Group welcome signer is not an admin"
                }
                check(authority.encryptionPublicKey.contentEquals(expectedOwnerEncryptionPublicKey)) {
                    "Group admin encryption key does not match the authenticated contact"
                }
            }

            groupCrypto
                .verify(
                    payload = payloadEncoder.encodeWelcome(packet),
                    signature = packet.ownerSignature,
                    signingPublicKey = expectedOwnerSigningPublicKey
                ).getOrThrow()

            if (existingState != null && packet.epoch == existingState.currentEpoch) {
                check(
                    existingState.currentEpoch == packet.epoch &&
                        existingState.welcomePacketId == packet.packetId &&
                        existingState.localSigningPublicKey.contentEquals(localSigningPublicKey)
                ) {
                    "Group welcome conflicts with existing security state"
                }
            }

            check(
                packet.members.any { member ->
                    member.signingPublicKey.contentEquals(localSigningPublicKey) &&
                        member.encryptionPublicKey.contentEquals(localEncryptionKeyPair.publicKey)
                }
            ) {
                "Local identity is not a member of this group"
            }

            val groupKey =
                if (existingState == null || packet.epoch > existingState.currentEpoch) {
                    groupCrypto
                        .unwrapGroupKey(
                            wrappedGroupKey = packet.wrappedGroupKey,
                            localEncryptionPublicKey = localEncryptionKeyPair.publicKey,
                            localEncryptionPrivateKey = localEncryptionKeyPair.privateKey
                        ).getOrThrow()
                } else {
                    groupKeyRepository
                        .load(packet.groupId, packet.epoch)
                        .getOrThrow()
                        ?: error("Existing group key was not found")
                }

            OpenedGroupWelcome(
                packet = packet,
                groupKey = groupKey
            )
        }

    suspend fun persistJoinedGroup(
        openedWelcome: OpenedGroupWelcome,
        ownerContactId: String,
        authoritySigningPublicKey: ByteArray,
        localSigningPublicKey: ByteArray,
        memberKeys: List<GroupMemberKeyEntity>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val packet = openedWelcome.packet
            val localMember =
                packet.members.single { member ->
                    member.signingPublicKey.contentEquals(localSigningPublicKey)
                }
            val authority =
                packet.members.firstOrNull { member ->
                    member.signingPublicKey.contentEquals(authoritySigningPublicKey)
                }
            val authorityLeft =
                authority == null &&
                    packet.membershipChange?.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
                    packet.membershipChange?.memberSigningPublicKey.contentEquals(authoritySigningPublicKey)
            check(authority?.role?.isGroupAdminRole() == true || authorityLeft) {
                "Group welcome authority is not an admin"
            }

            groupKeyRepository
                .save(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    groupKey = openedWelcome.groupKey
                ).getOrThrow()

            val existingState = groupSecurityDao.findState(packet.groupId)
            val joinedState =
                GroupSecurityStateEntity(
                    groupId = packet.groupId,
                    currentEpoch = packet.epoch,
                    welcomePacketId = packet.packetId,
                    ownerContactId = ownerContactId,
                    ownerSigningPublicKey = authority?.signingPublicKey?.copyOf() ?: authoritySigningPublicKey.copyOf(),
                    localSigningPublicKey = localSigningPublicKey.copyOf(),
                    localRole = localMember.role,
                    updatedAtEpochMilliseconds = receivedAtEpochMilliseconds
                )
            if (existingState == null || packet.epoch > existingState.currentEpoch) {
                groupSecurityDao.replaceCurrentEpoch(
                    state = joinedState,
                    memberKeys = memberKeys
                )
            } else {
                check(
                    existingState.currentEpoch == joinedState.currentEpoch &&
                        existingState.welcomePacketId == joinedState.welcomePacketId &&
                        existingState.ownerContactId == joinedState.ownerContactId &&
                        existingState.localSigningPublicKey.contentEquals(joinedState.localSigningPublicKey) &&
                        existingState.localRole == joinedState.localRole
                ) {
                    "Repeated group welcome conflicts with the installed group state"
                }
                groupSecurityDao.upsertMemberKeys(memberKeys)
            }

            groupKeyRepository
                .deleteBefore(
                    groupId = packet.groupId,
                    epoch = packet.epoch
                ).getOrThrow()
        }

    private companion object {
        const val INITIAL_EPOCH = 1
    }
}
