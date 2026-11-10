package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendMessagingIndicatorUseCase

/** Orchestration resolves conversation-specific recipients; Messaging sends to each contact's mailbox. */
class SendConversationIndicatorUseCase(
    private val groupRoutingResolver: GroupRoutingResolver,
    private val sendIndicator: SendMessagingIndicatorUseCase
) {
    suspend fun toContact(contactId: String, indicatorType: String): Result<Unit> =
        safeSuspendCall(logFailure = false) { sendIndicator(contactId, null, indicatorType).getOrThrow() }

    suspend fun toGroup(groupId: String, indicatorType: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val memberContactIds = groupRoutingResolver.resolveIndicatorMembers(groupId).keys
            var firstFailure: Throwable? = null
            memberContactIds.forEach { contactId ->
                sendIndicator(contactId, groupId, indicatorType).exceptionOrNull()?.let { error ->
                    if (firstFailure == null) firstFailure = error
                }
            }
            firstFailure?.let { throw it }
        }
}
