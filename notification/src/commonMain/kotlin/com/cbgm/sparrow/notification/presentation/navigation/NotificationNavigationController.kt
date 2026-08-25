package com.cbgm.sparrow.notification.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationNavigationController {
    private val mutablePendingTarget = MutableStateFlow<NotificationNavigationTarget?>(null)

    val pendingTarget: StateFlow<NotificationNavigationTarget?> = mutablePendingTarget.asStateFlow()

    fun openConversation(conversationId: String) {
        require(conversationId.isNotBlank()) {
            "Conversation ID must not be blank"
        }

        mutablePendingTarget.value =
            NotificationNavigationTarget.Conversation(
                conversationId = conversationId
            )
    }

    fun consume(target: NotificationNavigationTarget) {
        if (mutablePendingTarget.value == target) {
            mutablePendingTarget.value = null
        }
    }
}
