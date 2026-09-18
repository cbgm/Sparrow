package com.cbgm.sparrow.feature.membership.data.datasource

internal class GroupMembershipAttemptDataSource(
    private val verificationDataSource: GroupMembershipVerificationDataSource
) {
    suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            verificationDataSource.initializeOwnedGroup(groupId).getOrThrow()
        }
}
