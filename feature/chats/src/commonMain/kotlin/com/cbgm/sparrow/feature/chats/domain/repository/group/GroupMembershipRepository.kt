package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupInvitationOwnerIdentity
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import kotlinx.coroutines.flow.Flow

interface GroupMembershipRepository {
    fun observeAdministration(groupId: String): Flow<GroupAdministrationState>

    suspend fun create(
        title: String,
        contactIds: Set<String>
    ): Result<String>

    suspend fun addMembers(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit>

    suspend fun removeMember(
        groupId: String,
        contactId: String
    ): Result<Unit>

    suspend fun promoteMember(
        groupId: String,
        contactId: String
    ): Result<Unit>

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String
    ): Result<Unit>

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement>

    suspend fun leave(groupId: String): Result<Unit>

    suspend fun delete(groupId: String): Result<Unit>

    suspend fun getIncomingInvitationOwnerIdentity(groupId: String): Result<GroupInvitationOwnerIdentity>

    suspend fun acceptInvitation(groupId: String): Result<Unit>

    suspend fun declineInvitation(groupId: String): Result<Unit>
}
