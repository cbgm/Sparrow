package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupMessageMembershipAccessUseCase

class CloseGroupPollUseCase(
    private val repository: GroupMessageRepository,
    private val getMessageMembershipAccess: GetGroupMessageMembershipAccessUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        messageId: String,
        pollId: String
    ): Result<Unit> {
        val access = getMessageMembershipAccess(groupId).getOrElse { return Result.failure(it) }
        return repository.closePoll(
            groupId = groupId,
            messageId = messageId,
            pollId = pollId,
            closedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
            access = access
        )
    }
}
