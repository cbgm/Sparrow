package com.cbgm.sparrow.feature.conversationorchestration.delivery.runtime.indicator

import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationIndicator
import com.cbgm.sparrow.feature.conversationorchestration.api.observer.ConversationIndicatorObserver
import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.domain.usecase.ObserveMessagingIndicatorsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/** Matches indicators to a conversation member: the sender is the contact whose mailbox they arrived in. */
internal class TransportConversationIndicatorObserver(
    private val observeIndicators: ObserveMessagingIndicatorsUseCase
) : ConversationIndicatorObserver {
    override fun observeContact(contactId: String): Flow<ConversationIndicator> =
        observeIndicators()
            .filter { event -> event.senderContactId == contactId && event.groupId == null }
            .map(MessagingIndicator::toConversationIndicator)

    override fun observeGroupMember(groupId: String, contactId: String): Flow<ConversationIndicator> =
        observeIndicators()
            .filter { event -> event.senderContactId == contactId && event.groupId == groupId }
            .map(MessagingIndicator::toConversationIndicator)
}

private fun MessagingIndicator.toConversationIndicator(): ConversationIndicator =
    ConversationIndicator.entries.firstOrNull { indicator -> indicator.name == indicatorType } ?: ConversationIndicator.NONE
