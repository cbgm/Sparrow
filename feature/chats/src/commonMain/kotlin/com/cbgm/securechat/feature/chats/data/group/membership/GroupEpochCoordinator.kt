package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.group.security.GROUP_MEMBER_ROLE
import com.cbgm.securechat.feature.chats.data.group.security.GROUP_OWNER_ROLE
import com.cbgm.securechat.feature.chats.data.group.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.group.security.GroupWelcomeRecipient
import com.cbgm.securechat.feature.contacts.domain.model.Contact

internal class GroupEpochCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityManager: GroupSecurityManager,
    private val identity: GroupMembershipIdentity
) {
    suspend fun findCurrentParticipants(groupId: String): List<ConversationParticipantEntity> =
        chatDao.findConversationParticipants(groupId).mapNotNull { participant ->
            val contactIdentity =
                identity.requireContact(participant.contactId).secureChatIdentity
                    ?: return@mapNotNull null
            val memberKey =
                groupSecurityManager
                    .findRemoteMemberKey(
                        groupId = groupId,
                        contactId = participant.contactId
                    ).getOrNull()
                    ?: return@mapNotNull null
            if (!memberKey.signingPublicKey.contentEquals(contactIdentity.signingPublicKey)) {
                return@mapNotNull null
            }
            participant.copy(role = memberKey.role)
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
        val rolesByContactId = currentEpochRoles(groupId, contacts)

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
                val contactIdentity = requireNotNull(contact.secureChatIdentity)
                add(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = contactIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = contactIdentity.signingPublicKey.copyOf(),
                        role =
                            roleOverrides[contact.id]
                                ?: rolesByContactId[contact.id]
                                ?: GROUP_MEMBER_ROLE,
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
    ): List<GroupMemberKeyEntity> {
        val rolesByContactId = currentEpochRoles(groupId, contacts)

        return contacts.map { contact ->
            val contactIdentity = requireNotNull(contact.secureChatIdentity)
            GroupMemberKeyEntity(
                groupId = groupId,
                epoch = epoch,
                contactId = contact.id,
                encryptionPublicKey = contactIdentity.encryptionPublicKey.copyOf(),
                signingPublicKey = contactIdentity.signingPublicKey.copyOf(),
                role =
                    roleOverrides[contact.id]
                        ?: rolesByContactId[contact.id]
                        ?: GROUP_MEMBER_ROLE
            )
        }
    }

    suspend fun createRecipients(
        groupId: String,
        contacts: List<Contact>
    ): List<GroupWelcomeRecipient> =
        contacts.map { contact ->
            val invitation = groupInvitationDao.findByGroupAndContact(groupId, contact.id)
            GroupWelcomeRecipient(
                contactId = contact.id,
                invitationId = invitation?.invitationId ?: "member-${contact.id}",
                encryptionPublicKey =
                    requireNotNull(contact.secureChatIdentity)
                        .encryptionPublicKey
                        .copyOf()
            )
        }

    private suspend fun currentEpochRoles(
        groupId: String,
        contacts: List<Contact>
    ): Map<String, String> =
        contacts.mapNotNull { contact ->
            groupSecurityManager
                .findRemoteMemberKey(groupId, contact.id)
                .getOrNull()
                ?.let { memberKey -> contact.id to memberKey.role }
        }.toMap()
}
