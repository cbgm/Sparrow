package com.cbgm.sparrow.notification.domain.usecase

import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.sparrow.notification.domain.model.NotificationConversationTarget
import kotlinx.coroutines.flow.first

class ResolveNotificationConversationUseCase(
    private val conversationOverviewRepository: ConversationOverviewRepository
) {
    suspend operator fun invoke(conversationId: String): NotificationConversationTarget? {
        val conversation =
            conversationOverviewRepository
                .observeAll()
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
