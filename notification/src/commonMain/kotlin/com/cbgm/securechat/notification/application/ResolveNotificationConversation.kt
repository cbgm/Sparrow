package com.cbgm.securechat.notification.application

import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.securechat.feature.chats.domain.usecase.overview.ObserveConversationOverviewsUseCase
import com.cbgm.securechat.notification.model.NotificationConversationTarget
import kotlinx.coroutines.flow.first

class ResolveNotificationConversation(
    private val observeConversations: ObserveConversationOverviewsUseCase
) {
    suspend operator fun invoke(conversationId: String): NotificationConversationTarget? {
        val conversation =
            observeConversations()
                .first()
                .firstOrNull { conversation ->
                    conversation.id == conversationId
                } ?: return null

        return if (conversation.type == ConversationOverviewType.GROUP) {
            NotificationConversationTarget.Group(conversationId = conversation.id)
        } else {
            NotificationConversationTarget.Direct(
                conversationId = conversation.id,
                contactId = conversation.contactId,
                contactName = conversation.displayName
            )
        }
    }
}
