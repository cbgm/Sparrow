package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupMessageMembershipAccessUseCase

class DeleteGroupMessageUseCase(
    private val repository: GroupMessageRepository,
    private val getMessageMembershipAccess: GetGroupMessageMembershipAccessUseCase
) {
    suspend operator fun invoke(groupId: String, messageId: String): Result<Unit> {
        val access = getMessageMembershipAccess(groupId).getOrElse { return Result.failure(it) }
        return repository.deleteMessage(groupId, messageId, access)
    }
}
