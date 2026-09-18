package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.membership.data.model.GROUP_MEMBER_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GROUP_OWNER_ROLE
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipParticipantDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPeerDto
import com.cbgm.sparrow.feature.membership.data.model.GroupWelcomeRecipientDto

internal class GroupEpochDataSource(
    private val membershipStore: GroupMembershipStoreDataSource,
    private val securityStore: GroupSecurityStoreDataSource,
    private val groupSecurityManager: GroupMembershipSecurityDataSource
) {
    suspend fun findCurrentParticipants(groupId: String): List<GroupMembershipParticipantDto> {
        val state = securityStore.findState(groupId) ?: return emptyList()
        return securityStore
            .findMemberKeys(groupId, state.currentEpoch)
            .map { memberKey ->
                GroupMembershipParticipantDto(
                    contactId = memberKey.contactId,
                    role = memberKey.role,
                    joinedAtEpochMilliseconds = state.updatedAtEpochMilliseconds
                )
            }
    }

    suspend fun loadCurrentParticipantContacts(groupId: String): List<GroupMembershipPeerDto> {
        val state = securityStore.findState(groupId) ?: return emptyList()
        return securityStore
            .findMemberKeys(groupId, state.currentEpoch)
            .map { memberKey ->
                GroupMembershipPeerDto(
                    id = memberKey.contactId,
                    displayName = null,
                    preferredPhoneNumber = null,
                    phoneNumbers = emptyList(),
                    encryptionPublicKey = memberKey.encryptionPublicKey.copyOf(),
                    signingPublicKey = memberKey.signingPublicKey.copyOf(),
                    hasMutualIdentity = true
                )
            }
    }

    suspend fun createMemberPayloads(
        groupId: String,
        localIdentity: LocalPublicIdentity,
        localPhoneNumber: String,
        contacts: List<GroupMembershipPeerDto>,
        roleOverrides: Map<String, String> = emptyMap()
    ): List<GroupMemberPayload> {
        val localRole =
            groupSecurityManager.findLocalRole(groupId).getOrThrow()
                ?: GROUP_OWNER_ROLE

        return buildList {
            add(
                GroupMemberPayload(
                    displayName = null,
                    encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                    signingPublicKey = localIdentity.signingPublicKey.copyOf(),
                    role = localRole,
                    phoneNumber = localPhoneNumber
                )
            )
            contacts.forEach { contact ->
                val member = resolveMemberIdentity(groupId, contact)
                add(
                    GroupMemberPayload(
                        displayName = contact.displayName,
                        encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                        signingPublicKey = member.signingPublicKey.copyOf(),
                        role = roleOverrides[contact.id] ?: member.role,
                        phoneNumber = contact.preferredPhoneNumber ?: contact.phoneNumbers.firstOrNull()
                    )
                )
            }
        }
    }

    suspend fun createMemberKeys(
        groupId: String,
        epoch: Int,
        contacts: List<GroupMembershipPeerDto>,
        roleOverrides: Map<String, String> = emptyMap()
    ): List<GroupMemberKeyEntity> =
        contacts.map { contact ->
            val member = resolveMemberIdentity(groupId, contact)
            GroupMemberKeyEntity(
                groupId = groupId,
                epoch = epoch,
                contactId = contact.id,
                encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                signingPublicKey = member.signingPublicKey.copyOf(),
                role = roleOverrides[contact.id] ?: member.role
            )
        }

    suspend fun createRecipients(
        groupId: String,
        contacts: List<GroupMembershipPeerDto>
    ): List<GroupWelcomeRecipientDto> =
        contacts.map { contact ->
            val membership = membershipStore.findByGroupAndContact(groupId, contact.id)
            val member = resolveMemberIdentity(groupId, contact)
            GroupWelcomeRecipientDto(
                contactId = contact.id,
                invitationId = membership?.sourceInvitationId ?: "member-${contact.id}",
                encryptionPublicKey = member.encryptionPublicKey.copyOf()
            )
        }

    private suspend fun resolveMemberIdentity(
        groupId: String,
        contact: GroupMembershipPeerDto
    ): MemberIdentityDto {
        val currentMemberKey = currentMemberKey(groupId, contact.id)
        if (currentMemberKey != null) {
            return MemberIdentityDto(
                encryptionPublicKey = currentMemberKey.encryptionPublicKey,
                signingPublicKey = currentMemberKey.signingPublicKey,
                role = currentMemberKey.role
            )
        }

        val encryptionPublicKey =
            requireNotNull(contact.encryptionPublicKey) {
                "New group member has no accepted Sparrow encryption identity"
            }
        val signingPublicKey =
            requireNotNull(contact.signingPublicKey) {
                "New group member has no accepted Sparrow signing identity"
            }
        return MemberIdentityDto(
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey,
            role = GROUP_MEMBER_ROLE
        )
    }

    private suspend fun currentMemberKey(
        groupId: String,
        contactId: String
    ): GroupMemberKeyEntity? =
        groupSecurityManager
            .findRemoteMemberKey(
                groupId = groupId,
                contactId = contactId
            ).getOrNull()

    private data class MemberIdentityDto(
        val encryptionPublicKey: ByteArray,
        val signingPublicKey: ByteArray,
        val role: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MemberIdentityDto) return false
            return encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
                signingPublicKey.contentEquals(other.signingPublicKey) &&
                role == other.role
        }

        override fun hashCode(): Int {
            var result = encryptionPublicKey.contentHashCode()
            result = 31 * result + signingPublicKey.contentHashCode()
            result = 31 * result + role.hashCode()
            return result
        }
    }
}
