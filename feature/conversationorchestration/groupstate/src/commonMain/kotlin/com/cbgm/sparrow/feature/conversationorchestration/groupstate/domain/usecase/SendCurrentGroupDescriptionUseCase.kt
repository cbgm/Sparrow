package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupDescriptionRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase

/** Replays the published group description to one member that just became ready. */
class SendCurrentGroupDescriptionUseCase(
    private val repository: GroupDescriptionRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createPackets: CreateGroupDescriptionPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) {
    suspend operator fun invoke(groupId: String, contactId: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val state = repository.get(groupId).getOrThrow()
            if (!state.isPublished) return@safeSuspendCall
            val context = requireGroupAdminContext.forRecipient(groupId, ADMIN_ACTION, contactId).getOrThrow()
            val packets = createPackets(state, context, recipients = setOf(contactId)).getOrThrow()
            enqueueGroupPackets(packets, recordFailedRecipients = false).getOrThrow()
        }

    private companion object {
        const val ADMIN_ACTION = "group description"
    }
}
