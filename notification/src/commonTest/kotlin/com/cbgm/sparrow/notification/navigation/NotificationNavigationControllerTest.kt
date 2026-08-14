package com.cbgm.sparrow.notification.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationNavigationControllerTest {
    private val controller = NotificationNavigationController()

    @Test
    fun openingConversationPublishesPendingTarget() {
        controller.openConversation("conversation-1")

        assertEquals(
            expected = NotificationNavigationTarget.Conversation("conversation-1"),
            actual = controller.pendingTarget.value
        )
    }

    @Test
    fun consumingCurrentTargetClearsIt() {
        val target = NotificationNavigationTarget.Conversation("conversation-1")

        controller.openConversation(target.conversationId)
        controller.consume(target)

        assertNull(controller.pendingTarget.value)
    }

    @Test
    fun consumingOlderTargetKeepsNewerTarget() {
        val olderTarget = NotificationNavigationTarget.Conversation("conversation-1")
        val newerTarget = NotificationNavigationTarget.Conversation("conversation-2")

        controller.openConversation(olderTarget.conversationId)
        controller.openConversation(newerTarget.conversationId)
        controller.consume(olderTarget)

        assertEquals(
            expected = newerTarget,
            actual = controller.pendingTarget.value
        )
    }
}
