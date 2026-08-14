package com.cbgm.sparrow.notification.navigation

sealed interface NotificationNavigationTarget {
    data class Conversation(
        val conversationId: String
    ) : NotificationNavigationTarget
}
