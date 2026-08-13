package com.cbgm.securechat.notification.application

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationsUseCase
import com.cbgm.securechat.notification.model.ConversationNotification
import com.cbgm.securechat.notification.model.ConversationNotificationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ObserveConversationNotificationEvents(
    private val observeConversations: ObserveConversationsUseCase,
    private val appVisibilityState: AppVisibilityState
) {
    operator fun invoke(): Flow<ConversationNotificationEvent> =
        flow {
            var unreadCounts: Map<String, Int> = emptyMap()
            var hasInitialSnapshot = false

            observeConversations().collect { conversations ->
                val nextUnreadCounts =
                    conversations.associate { conversation ->
                        conversation.id to conversation.unreadCount
                    }

                if (!hasInitialSnapshot) {
                    unreadCounts = nextUnreadCounts
                    hasInitialSnapshot = true
                    return@collect
                }

                unreadCounts.keys
                    .minus(nextUnreadCounts.keys)
                    .forEach { conversationId ->
                        emit(
                            ConversationNotificationEvent.Cancel(
                                conversationId = conversationId
                            )
                        )
                    }

                conversations.forEach { conversation ->
                    val previousUnreadCount = unreadCounts[conversation.id] ?: 0

                    when {
                        conversation.unreadCount == 0 && previousUnreadCount > 0 -> {
                            emit(
                                ConversationNotificationEvent.Cancel(
                                    conversationId = conversation.id
                                )
                            )
                        }

                        conversation.unreadCount > previousUnreadCount &&
                            !appVisibilityState.isVisible.value -> {
                            emit(
                                ConversationNotificationEvent.Show(
                                    notification = conversation.toNotification()
                                )
                            )
                        }
                    }
                }

                unreadCounts = nextUnreadCounts
            }
        }

    private fun Conversation.toNotification(): ConversationNotification =
        ConversationNotification(
            conversationId = id,
            title = contactName,
            messagePreview = lastMessage?.text?.takeIf(String::isNotBlank),
            unreadCount = unreadCount
        )
}
