package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMessageRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.GetGroupMessageMembershipAccessUseCase

class SendGroupMessageUseCase(
    private val repository: GroupMessageRepository,
    private val getMessageMembershipAccess: GetGroupMessageMembershipAccessUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        parts: List<MessagePart>,
        replyToMessageId: String? = null
    ): Result<Unit> {
        val access = getMessageMembershipAccess(groupId).getOrElse { return Result.failure(it) }
        return repository.send(groupId, parts, replyToMessageId, access)
    }
}
