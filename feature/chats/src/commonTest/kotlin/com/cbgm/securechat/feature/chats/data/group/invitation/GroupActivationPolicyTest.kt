package com.cbgm.securechat.feature.chats.data.group.invitation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupActivationPolicyTest {
    @Test
    fun activeMembersReceiveGroupMessages() {
        assertTrue(canSendToActiveGroupMembers(activeParticipantCount = 1))
    }

    @Test
    fun groupWithoutActiveMembersHasNoRecipients() {
        assertFalse(canSendToActiveGroupMembers(activeParticipantCount = 0))
    }
}
