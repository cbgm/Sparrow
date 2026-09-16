package com.cbgm.sparrow.feature.membership.data.datasource

interface GroupMembershipVerificationDataSource {
    suspend fun initializeOwnedGroup(groupId: String): Result<Unit>

    suspend fun onOwnedMembershipChanged(groupId: String): Result<Unit>
}
