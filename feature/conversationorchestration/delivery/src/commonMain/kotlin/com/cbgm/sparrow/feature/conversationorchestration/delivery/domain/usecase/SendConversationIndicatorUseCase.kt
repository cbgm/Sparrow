package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.messaging.domain.usecase.SendMessagingIndicatorUseCase

/** Orchestration resolves conversation-specific recipients; Messaging only sends routing IDs. */
class SendConversationIndicatorUseCase(
    private val resolveContactRoutingId: ResolveContactTransportRoutingIdUseCase,
    private val groupRoutingResolver: GroupRoutingResolver,
    private val sendIndicator: SendMessagingIndicatorUseCase
) {
    suspend fun toContact(contactId: String, indicatorType: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val routingId = resolveContactRoutingId(contactId).getOrThrow()
            sendIndicator(routingId, indicatorType).getOrThrow()
        }

    suspend fun toGroup(groupId: String, indicatorType: String): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val recipientRoutingIds = groupRoutingResolver.resolveIndicatorMembers(groupId).values
            var firstFailure: Throwable? = null
            recipientRoutingIds.forEach { routingId ->
                sendIndicator(routingId, indicatorType).exceptionOrNull()?.let { error ->
                    if (firstFailure == null) firstFailure = error
                }
            }
            firstFailure?.let { throw it }
        }
}
