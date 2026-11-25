package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.OpenedIncomingGroupWelcome
import com.cbgm.sparrow.feature.membership.domain.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentitySource
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket

/**
 * Membership authenticates the sender and group epoch before any Contacts or Chats writes. The first welcome is
 * verified against the pinned inviter identity, later welcomes against the admin of the prior epoch.
 */
class OpenIncomingGroupWelcomeUseCase(
    private val security: GroupSecurityRepository,
    private val localPublicIdentitySource: LocalPublicIdentitySource,
    private val localEncryptionKeyPairSource: LocalEncryptionKeyPairSource
) {
    suspend operator fun invoke(
        packet: GroupCreatedPacket,
        senderContactId: String,
        authorization: GroupIncomingWelcomeAuthorization,
        pinnedAuthorityEncryptionPublicKey: ByteArray?,
        pinnedAuthoritySigningPublicKey: ByteArray?
    ): Result<OpenedIncomingGroupWelcome> = safeSuspendCall(logFailure = false) {
        val isFirstWelcome = authorization.isFirstWelcome
        val previousAuthorityEncryptionPublicKey =
            pinnedAuthorityEncryptionPublicKey ?: authorization.priorAdminEncryptionPublicKey
        val previousAuthoritySigningPublicKey = pinnedAuthoritySigningPublicKey ?: authorization.priorAdminSigningPublicKey
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
        val local = localPublicIdentitySource.getLocalPublicIdentity().getOrThrow()
        val localKeyPair = localEncryptionKeyPairSource.getEncryptionKeyPair().getOrThrow()
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
