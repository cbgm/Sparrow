package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/** The active participants an operation must reach; an operation without any recipient is refused. */
class FindGroupRecipientsUseCase(
    private val messageRepository: GroupMessageRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<String>> =
        safeSuspendCall(logFailure = false) {
            val recipients = messageRepository.findParticipantIds(groupId).getOrThrow()
            check(recipients.isNotEmpty()) { "Group has no active recipients" }
            recipients
        }
}
