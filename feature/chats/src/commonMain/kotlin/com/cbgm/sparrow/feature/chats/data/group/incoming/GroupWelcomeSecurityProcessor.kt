package com.cbgm.sparrow.feature.chats.data.group.incoming

import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.outbox.ProtocolOutbox
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.feature.chats.data.group.protocol.GroupMembershipPacketProtocol
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.sparrow.feature.chats.domain.usecase.group.incoming.MarkGroupContactIdentityMutualUseCase

internal class GroupWelcomeSecurityProcessor(
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupSecurityDao: GroupSecurityDao,
    private val membershipPacketProtocol: GroupMembershipPacketProtocol,
    private val protocolOutbox: ProtocolOutbox,
    private val markGroupContactIdentityMutual: MarkGroupContactIdentityMutualUseCase
) {
    suspend fun openAndTrustWelcome(
        packet: GroupCreatedPacket,
        senderContactId: String,
        isFirstWelcome: Boolean
    ): GroupWelcomeContextDto {
        val authorityIdentity =
            resolveAuthorityIdentity(
                groupId = packet.groupId,
                senderContactId = senderContactId,
                isFirstWelcome = isFirstWelcome
            )
        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localEncryptionKeyPair = localEncryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()
        val openedWelcome =
            groupSecurityManager
                .openWelcome(
                    packet = packet,
                    senderContactId = senderContactId,
                    expectedOwnerEncryptionPublicKey = authorityIdentity.encryptionPublicKey,
                    expectedOwnerSigningPublicKey = authorityIdentity.signingPublicKey,
                    localEncryptionKeyPair = localEncryptionKeyPair,
                    localSigningPublicKey = localIdentity.signingPublicKey
                ).getOrThrow()

        if (isFirstWelcome) {
            markGroupContactIdentityMutual(
                contactId = senderContactId,
                encryptionPublicKey = authorityIdentity.encryptionPublicKey,
                signingPublicKey = authorityIdentity.signingPublicKey
            ).getOrThrow()
        }

        return GroupWelcomeContextDto(
            authorityIdentity = authorityIdentity,
            localIdentity = localIdentity,
            openedWelcome = openedWelcome
        )
    }

    fun validateAuthorityAndResolveReferenceAdmin(
        packet: GroupCreatedPacket,
        senderContactId: String,
        welcome: GroupWelcomeContextDto,
        membership: ResolvedGroupMembershipDto
    ): GroupWelcomeReferenceAdminDto {
        val membershipChange = packet.membershipChange
        val authorityLeft =
            membershipChange != null &&
                membershipChange.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
                membershipChange.memberSigningPublicKey.contentEquals(welcome.authorityIdentity.signingPublicKey)

        val senderIsAdmin =
            membership.memberKeys.any { member ->
                member.contactId == senderContactId && member.role.isGroupAdminRole()
            }
        check(authorityLeft || senderIsAdmin) { "Authenticated sender is not a group admin" }

        if (!authorityLeft) {
            return GroupWelcomeReferenceAdminDto(
                contactId = senderContactId,
                signingPublicKey = welcome.authorityIdentity.signingPublicKey
            )
        }

        val continuingAdmin = membership.memberKeys.firstOrNull { it.role.isGroupAdminRole() }
        return GroupWelcomeReferenceAdminDto(
            contactId = continuingAdmin?.contactId ?: senderContactId,
            signingPublicKey = continuingAdmin?.signingPublicKey ?: welcome.authorityIdentity.signingPublicKey
        )
    }

    suspend fun persistGroupSecurity(
        welcome: GroupWelcomeContextDto,
        membership: ResolvedGroupMembershipDto,
        referenceAdmin: GroupWelcomeReferenceAdminDto,
        persistedAt: Long
    ) {
        groupSecurityManager
            .persistJoinedGroup(
                openedWelcome = welcome.openedWelcome,
                ownerContactId = referenceAdmin.contactId,
                authoritySigningPublicKey = referenceAdmin.signingPublicKey,
                localSigningPublicKey = welcome.localIdentity.signingPublicKey,
                memberKeys = membership.memberKeys,
                receivedAtEpochMilliseconds = persistedAt
            ).getOrThrow()
    }

    suspend fun sendReadyAcknowledgement(
        packet: GroupCreatedPacket,
        senderContactId: String,
        welcome: GroupWelcomeContextDto
    ) {
        val readyAcknowledgement =
            membershipPacketProtocol
                .createReadyAcknowledgement(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    welcomePacketId = packet.packetId,
                    keyConfirmation =
                        groupSecurityManager.createKeyConfirmation(
                            groupId = packet.groupId,
                            epoch = packet.epoch,
                            groupKey = welcome.openedWelcome.groupKey
                        ),
                    memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                ).getOrThrow()
        protocolOutbox.enqueue(senderContactId, readyAcknowledgement).getOrThrow()
    }

    private suspend fun resolveAuthorityIdentity(
        groupId: String,
        senderContactId: String,
        isFirstWelcome: Boolean
    ): GroupWelcomeAuthorityIdentityDto {
        if (isFirstWelcome) {
            val contactIdentity =
                contactDao.findPublicIdentityByContactId(senderContactId)
                    ?: error("Inviting group admin has no accepted Sparrow identity")
            return GroupWelcomeAuthorityIdentityDto(
                encryptionPublicKey = contactIdentity.encryptionPublicKey.copyOf(),
                signingPublicKey = contactIdentity.signingPublicKey.copyOf()
            )
        }

        val state =
            groupSecurityDao.findState(groupId)
                ?: error("Group security state was not found")
        val memberKey =
            groupSecurityDao.findMemberKey(
                groupId = groupId,
                epoch = state.currentEpoch,
                contactId = senderContactId
            ) ?: error("Group update sender is not part of the current epoch")
        check(memberKey.role.isGroupAdminRole()) {
            "Group update sender is not an admin"
        }
        return GroupWelcomeAuthorityIdentityDto(
            encryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
            signingPublicKey = memberKey.signingPublicKey.copyOf()
        )
    }
}
