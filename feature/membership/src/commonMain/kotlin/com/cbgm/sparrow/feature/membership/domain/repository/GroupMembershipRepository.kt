package com.cbgm.sparrow.feature.membership.domain.repository

import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
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
}
