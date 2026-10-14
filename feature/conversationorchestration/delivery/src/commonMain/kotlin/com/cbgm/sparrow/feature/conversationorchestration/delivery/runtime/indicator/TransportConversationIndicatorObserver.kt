package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.contacts.domain.usecase.ResolveContactTransportRoutingIdUseCase
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationIndicator
import com.cbgm.sparrow.feature.conversationorchestration.api.observer.ConversationIndicatorObserver
import com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.routing.GroupRoutingResolver
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingIndicatorsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/** Matches generic transport indicator events to a conversation member. */
internal class TransportConversationIndicatorObserver(
    private val resolveContactRoutingId: ResolveContactTransportRoutingIdUseCase,
    private val groupRoutingResolver: GroupRoutingResolver,
    private val observeIndicators: ObserveMessagingIndicatorsUseCase
) : ConversationIndicatorObserver {
    private val logger = SparrowLog.withTag("TransportConversationIndicatorObserver")

    override fun observeContact(contactId: String): Flow<ConversationIndicator> =
        observeForSender { resolveContactRoutingId(contactId).getOrThrow() }

    override fun observeGroupMember(groupId: String, contactId: String): Flow<ConversationIndicator> =
        observeForSender { groupRoutingResolver.resolve(groupId, contactId) }

    private fun observeForSender(resolveSenderRoutingId: suspend () -> String): Flow<ConversationIndicator> =
        observeIndicators().transform { event ->
            val expectedSender = try {
                resolveSenderRoutingId()
            } catch (error: CancellationException) {
                throw error
            } catch (failure: Throwable) {
                logger.error(failure) { "Could not resolve indicator sender" }
                return@transform
            }
            if (event.senderRoutingId == expectedSender) emit(event.indicatorType.toConversationIndicator())
        }

    private fun String.toConversationIndicator(): ConversationIndicator =
        ConversationIndicator.entries.firstOrNull { indicator -> indicator.name == this } ?: ConversationIndicator.NONE
}
