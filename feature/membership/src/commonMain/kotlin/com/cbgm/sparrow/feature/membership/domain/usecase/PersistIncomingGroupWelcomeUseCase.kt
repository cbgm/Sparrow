package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupWelcomeMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.OpenedIncomingGroupWelcome
import com.cbgm.sparrow.feature.membership.domain.model.isGroupAdminRole
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket

/**
 * Membership alone persists group epoch, member keys and the secured welcome. Only members resolved to a contact
 * ([contactIdsByMember], by member index) get keys. The sender owns the group, unless the admin left with this
 * welcome: then the member it named admin keeps the group.
 */
class PersistIncomingGroupWelcomeUseCase(
    private val security: GroupSecurityRepository
) {
    suspend operator fun invoke(
        welcome: OpenedIncomingGroupWelcome,
        packet: GroupCreatedPacket,
        senderContactId: String,
        contactIdsByMember: List<String?>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> = security.persistJoinedGroup(
        openedWelcome = welcome.openedWelcome,
        ownerContactId = continuingAdminId(welcome, packet, contactIdsByMember) ?: senderContactId,
        authoritySigningPublicKey = welcome.authoritySigningPublicKey,
        localSigningPublicKey = welcome.localSigningPublicKey,
        memberKeys = memberKeys(packet, contactIdsByMember),
        receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
    )

    private fun continuingAdminId(
        welcome: OpenedIncomingGroupWelcome,
        packet: GroupCreatedPacket,
        contactIdsByMember: List<String?>
    ): String? {
        if (!welcome.authorityLeft) return null
        return packet.members.indices.firstNotNullOfOrNull { index ->
            contactIdsByMember[index]?.takeIf { packet.members[index].role.isGroupAdminRole() }
        }
    }

    private fun memberKeys(packet: GroupCreatedPacket, contactIdsByMember: List<String?>): List<GroupWelcomeMemberKey> =
        packet.members.mapIndexedNotNull { index, member ->
            val contactId = contactIdsByMember[index] ?: return@mapIndexedNotNull null
            GroupWelcomeMemberKey(
                groupId = packet.groupId,
                epoch = packet.epoch,
                contactId = contactId,
                encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                signingPublicKey = member.signingPublicKey.copyOf(),
                role = member.role,
                phoneNumber = member.phoneNumber
            )
        }
}
