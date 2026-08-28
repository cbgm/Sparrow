package com.cbgm.sparrow.notification.presentation.navigation

sealed interface NotificationNavigationTarget {
    data class Conversation(
        val conversationId: String
    ) : NotificationNavigationTarget
}
