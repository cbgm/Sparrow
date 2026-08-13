package com.cbgm.securechat.feature.chats.data.group.repository

import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.feature.chats.data.group.membership.GroupMembershipCoordinator
import com.cbgm.securechat.feature.chats.data.group.security.isGroupAdminRole
import com.cbgm.securechat.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.securechat.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.transformLatest

class GroupMembershipRepositoryImpl(
    private val chatDao: ChatDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val membershipCoordinator: GroupMembershipCoordinator,
    private val getContact: GetContact
) : GroupMembershipRepository {
    override fun observeAdministration(groupId: String): Flow<GroupAdministrationState> =
        combine(
            chatDao.observeConversationParticipants(groupId),
            groupSecurityDao.observeState(groupId),
            groupSecurityDao.observeCurrentMemberKeys(groupId)
        ) { participants, securityState, memberKeys ->
            Triple(participants, securityState, memberKeys)
        }.transformLatest { (participants, securityState, memberKeys) ->
            if (securityState == null) {
                emit(GroupAdministrationState())
                return@transformLatest
            }

            val memberKeysByContactId = memberKeys.associateBy { it.contactId }
            val currentMembers = mutableSetOf<String>()
            val currentAdmins = mutableSetOf<String>()

            participants.forEach { participant ->
                val pinnedKey =
                    memberKeysByContactId[participant.contactId]
                        ?: return@forEach
                val identity =
                    getContact(participant.contactId)
                        .getOrNull()
                        ?.secureChatIdentity
                        ?: return@forEach
                if (!pinnedKey.signingPublicKey.contentEquals(identity.signingPublicKey)) {
                    return@forEach
                }

                currentMembers += participant.contactId
                if (pinnedKey.role.isGroupAdminRole()) {
                    currentAdmins += participant.contactId
                }
            }

            val localIsAdmin = securityState.localRole.isGroupAdminRole()
            emit(
                GroupAdministrationState(
                    isLocalAdmin = localIsAdmin,
                    isOrphaned = !localIsAdmin && currentAdmins.isEmpty(),
                    adminContactIds = currentAdmins,
                    currentMemberContactIds = currentMembers,
                    promotableContactIds =
                        currentMembers.filterTo(mutableSetOf()) { contactId ->
                            contactId !in currentAdmins
                        },
                    requiresPromotionBeforeLeave =
                        localIsAdmin && currentMembers.isNotEmpty() && currentAdmins.isEmpty(),
                    activeMemberCount = currentMembers.size + 1
                )
            )
        }

    override suspend fun create(
        title: String,
        contactIds: Set<String>
    ): Result<String> = membershipCoordinator.createGroup(title, contactIds)

    override suspend fun addMembers(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> = membershipCoordinator.addMembers(groupId, contactIds)

    override suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = membershipCoordinator.removeMember(groupId, contactId)

    override suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit> = membershipCoordinator.promoteMember(groupId, contactId)

    override suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit> = membershipCoordinator.transferAdminAndLeave(groupId, contactId)

    override suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement> =
        membershipCoordinator.getLeaveRequirement(groupId)

    override suspend fun leave(groupId: String): Result<Unit> =
        membershipCoordinator.leaveGroup(groupId)

    override suspend fun delete(groupId: String): Result<Unit> =
        membershipCoordinator.deleteGroupConversation(groupId)

    override suspend fun acceptInvitation(groupId: String): Result<Unit> =
        membershipCoordinator.acceptInvitation(groupId)

    override suspend fun declineInvitation(groupId: String): Result<Unit> =
        membershipCoordinator.declineInvitation(groupId)
}
