package com.cbgm.sparrow.feature.chats.runtime.group.verification

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationActionRepository

class GroupVerificationActionRepositoryImpl(
    private val verificationCoordinator: GroupVerificationCoordinator
) : GroupVerificationActionRepository {
    override suspend fun initializeOwnedGroup(groupId: String): Result<Unit> =
        verificationCoordinator.initializeOwnedGroup(groupId)

    override suspend fun synchronize(groupId: String): Result<Unit> =
        verificationCoordinator.synchronize(groupId)

    override suspend fun verify(
        groupId: String,
        contactId: String
    ): Result<Unit> = verificationCoordinator.verify(groupId, contactId)
}
