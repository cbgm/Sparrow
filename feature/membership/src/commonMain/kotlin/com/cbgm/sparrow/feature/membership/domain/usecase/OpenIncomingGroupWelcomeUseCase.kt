package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.feature.membership.domain.model.OpenedIncomingGroupWelcome
import com.cbgm.sparrow.feature.membership.domain.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository

/** Membership authenticates the sender and group epoch before any Contacts or Chats writes. */
class OpenIncomingGroupWelcomeUseCase(
    private val security: GroupSecurityRepository,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider
) {
    suspend operator fun invoke(
        packet: GroupCreatedPacket,
        senderContactId: String,
        isFirstWelcome: Boolean,
        previousAuthorityEncryptionPublicKey: ByteArray?,
        previousAuthoritySigningPublicKey: ByteArray?
    ): Result<OpenedIncomingGroupWelcome> = runCatching {
        val expectedEncryption = requireNotNull(previousAuthorityEncryptionPublicKey) {
            if (isFirstWelcome) {
                "Inviting group admin has no accepted Sparrow identity"
            } else {
                "Group update sender is not part of the current epoch"
            }
        }
        val expectedSigning = requireNotNull(previousAuthoritySigningPublicKey) {
            if (isFirstWelcome) {
                "Inviting group admin has no accepted Sparrow identity"
            } else {
                "Group update sender is not part of the current epoch"
            }
        }
        val local = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localKeyPair = localEncryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()
        val opened = security.openWelcome(
            packet = packet,
            senderContactId = senderContactId,
            expectedOwnerEncryptionPublicKey = expectedEncryption,
            expectedOwnerSigningPublicKey = expectedSigning,
            localEncryptionKeyPair = localKeyPair,
            localSigningPublicKey = local.signingPublicKey
        ).getOrThrow()
        val authorityLeft = packet.membershipChange?.let { change ->
            change.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT &&
                change.memberSigningPublicKey.contentEquals(expectedSigning)
        } == true
        val senderIsAdmin = packet.members.any { member ->
            member.signingPublicKey.contentEquals(expectedSigning) && member.role.isGroupAdminRole()
        }
        check(authorityLeft || senderIsAdmin) { "Authenticated sender is not a group admin" }
        OpenedIncomingGroupWelcome(
            openedWelcome = opened,
            localSigningPublicKey = local.signingPublicKey,
            authoritySigningPublicKey = expectedSigning.copyOf(),
            authorityLeft = authorityLeft
        )
    }
}
