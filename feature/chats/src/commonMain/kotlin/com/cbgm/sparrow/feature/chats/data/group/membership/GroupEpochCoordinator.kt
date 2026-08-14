package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.ConversationParticipantEntity
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_MEMBER_ROLE
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_OWNER_ROLE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.sparrow.feature.chats.data.group.security.GroupWelcomeRecipient
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

internal class GroupEpochCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupSecurityManager: GroupSecurityManager,
    private val identity: GroupMembershipIdentity
) {
    suspend fun findCurrentParticipants(groupId: String): List<ConversationParticipantEntity> {
        val state = groupSecurityDao.findState(groupId) ?: return emptyList()
        val existingByContactId =
            chatDao.findConversationParticipants(groupId).associateBy(ConversationParticipantEntity::contactId)
        return groupSecurityDao
            .findMemberKeys(groupId, state.currentEpoch)
            .map { memberKey ->
                existingByContactId[memberKey.contactId]
                    ?.copy(role = memberKey.role)
                    ?: ConversationParticipantEntity(
                        conversationId = groupId,
                        contactId = memberKey.contactId,
                        role = memberKey.role,
                        joinedAtEpochMilliseconds = state.updatedAtEpochMilliseconds
                    )
            }
    }

    suspend fun loadCurrentParticipantContacts(groupId: String): List<Contact> =
        findCurrentParticipants(groupId)
            .map { participant -> identity.requireContact(participant.contactId) }

    suspend fun createMemberPayloads(
        groupId: String,
        localIdentity: LocalPublicIdentity,
        localPhoneNumber: String,
        contacts: List<Contact>,
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
                        displayName = null,
                        encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                        signingPublicKey = member.signingPublicKey.copyOf(),
                        role = roleOverrides[contact.id] ?: member.role,
                        phoneNumber = contact.requireGroupPhoneNumber()
                    )
                )
            }
        }
    }

    suspend fun createMemberKeys(
        groupId: String,
        epoch: Int,
        contacts: List<Contact>,
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
        contacts: List<Contact>
    ): List<GroupWelcomeRecipient> =
        contacts.map { contact ->
            val invitation = groupInvitationDao.findByGroupAndContact(groupId, contact.id)
            val member = resolveMemberIdentity(groupId, contact)
            GroupWelcomeRecipient(
                contactId = contact.id,
                invitationId = invitation?.invitationId ?: "member-${contact.id}",
                encryptionPublicKey = member.encryptionPublicKey.copyOf()
            )
        }

    private suspend fun resolveMemberIdentity(
        groupId: String,
        contact: Contact
    ): MemberIdentity {
        val currentMemberKey = currentMemberKey(groupId, contact.id)
        if (currentMemberKey != null) {
            return MemberIdentity(
                encryptionPublicKey = currentMemberKey.encryptionPublicKey,
                signingPublicKey = currentMemberKey.signingPublicKey,
                role = currentMemberKey.role
            )
        }

        val contactIdentity =
            requireNotNull(contact.sparrowIdentity) {
                "New group member has no accepted Sparrow identity"
            }
        return MemberIdentity(
            encryptionPublicKey = contactIdentity.encryptionPublicKey,
            signingPublicKey = contactIdentity.signingPublicKey,
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

    private data class MemberIdentity(
        val encryptionPublicKey: ByteArray,
        val signingPublicKey: ByteArray,
        val role: String
    )
}
