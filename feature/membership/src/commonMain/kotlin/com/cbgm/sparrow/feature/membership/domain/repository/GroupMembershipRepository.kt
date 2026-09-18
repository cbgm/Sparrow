package com.cbgm.sparrow.feature.membership.domain.repository

import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import kotlinx.coroutines.flow.Flow

interface GroupMembershipRepository {
    fun observeAdministration(groupId: String): Flow<GroupAdministrationState>

    suspend fun initializeOwnedGroup(groupId: String): Result<Unit>

    suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult>

    suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult>

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<Unit>

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement>

    suspend fun leave(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Unit>

    suspend fun delete(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Unit>
}
