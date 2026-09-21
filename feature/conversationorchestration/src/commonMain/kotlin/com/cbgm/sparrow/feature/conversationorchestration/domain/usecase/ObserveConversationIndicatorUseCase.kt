package com.cbgm.sparrow.feature.conversationorchestration.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingIndicatorsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/** Matches generic transport events to a conversation member, without referencing Chats UI models. */
class ObserveConversationIndicatorUseCase(
    private val resolveContactRoutingId: ResolveContactTransportRoutingIdUseCase,
    private val groupRoutingResolver: GroupRoutingResolver,
    private val observeIndicators: ObserveMessagingIndicatorsUseCase
) {
    fun forContact(contactId: String): Flow<String> =
        observeForSender { resolveContactRoutingId(contactId) }

    fun forGroupMember(groupId: String, contactId: String): Flow<String> =
        observeForSender { groupRoutingResolver.resolve(groupId, contactId) }

    private fun observeForSender(resolveSenderRoutingId: suspend () -> String): Flow<String> =
        observeIndicators().transform { event ->
            val expectedSender = try {
                resolveSenderRoutingId()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                return@transform
            }
            if (event.senderRoutingId == expectedSender) emit(event.indicatorType)
        }
}
