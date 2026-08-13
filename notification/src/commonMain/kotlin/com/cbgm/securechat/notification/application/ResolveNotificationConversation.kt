package com.cbgm.securechat.notification.application

import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationsUseCase
import com.cbgm.securechat.notification.model.NotificationConversationTarget
import kotlinx.coroutines.flow.first

class ResolveNotificationConversation(
    private val observeConversations: ObserveConversationsUseCase
) {
    suspend operator fun invoke(conversationId: String): NotificationConversationTarget? {
        val conversation =
            observeConversations()
                .first()
                .firstOrNull { conversation ->
                    conversation.id == conversationId
                } ?: return null

        return if (conversation.isGroup) {
            NotificationConversationTarget.Group(conversationId = conversation.id)
        } else {
            NotificationConversationTarget.Direct(
                conversationId = conversation.id,
                contactId = conversation.contactId,
                contactName = conversation.contactName
            )
        }
    }
}
