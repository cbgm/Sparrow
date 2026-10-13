package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase

/** Replays the published group title to one member that just became ready. */
class SendCurrentGroupTitleUseCase(
    private val repository: GroupTitleRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createPackets: CreateGroupTitlePacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) {
    suspend operator fun invoke(groupId: String, contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val state = repository.get(groupId).getOrThrow()
            if (!state.isPublished) return@runCatching
            val context = requireGroupAdminContext(groupId, ADMIN_ACTION).getOrThrow()
            check(contactId in context.recipientContactIds) { "Contact is not an active group member" }
            val packets = createPackets(state, context, recipients = setOf(contactId)).getOrThrow()
            enqueueGroupPackets(packets, recordFailedRecipients = false).getOrThrow()
        }

    private companion object {
        const val ADMIN_ACTION = "group title"
    }
}
