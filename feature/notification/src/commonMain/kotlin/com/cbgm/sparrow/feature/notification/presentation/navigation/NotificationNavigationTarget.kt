package com.cbgm.sparrow.feature.notification.presentation.navigation

sealed interface NotificationNavigationTarget {
    data class Conversation(
        val conversationId: String
    ) : NotificationNavigationTarget
}
