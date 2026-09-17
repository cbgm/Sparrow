package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupLocalMembershipTimelineTest {
    @Test
    fun historyFromActivePeriodsIsKeptButAbsenceMessagesAreHidden() {
        val timeline =
            buildGroupLocalMembershipTimeline(
                messages =
                    listOf(
                        userMessage("before", 100L),
                        GroupMembershipMessageFactory.localMembershipLeft(
                            conversationId = GROUP_ID,
                            invitationId = "invite-1",
                            epoch = 2,
                            createdAtEpochMilliseconds = 200L
                        ),
                        userMessage("absent", 300L),
                        GroupMembershipMessageFactory.localMembershipStarted(
                            conversationId = GROUP_ID,
                            referenceId = "invite-2",
                            epoch = 4,
                            createdAtEpochMilliseconds = 400L
                        ),
                        userMessage("after", 500L)
                    ),
                memberships = emptyList()
            )

        assertEquals(
            listOf("before", "You left this group", "after"),
            timeline.visibleMessages.map(MessageEntity::text)
        )
        assertFalse(timeline.isLocallyInactive)
    }

    @Test
    fun reinviteAfterLeaveReopensMembershipStateButNotAbsenceHistory() {
        val timeline =
            buildGroupLocalMembershipTimeline(
                messages =
                    listOf(
                        userMessage("before", 100L),
                        GroupMembershipMessageFactory.localMembershipLeft(
                            conversationId = GROUP_ID,
                            invitationId = "invite-1",
                            epoch = 2,
                            createdAtEpochMilliseconds = 200L
                        ),
                        userMessage("absent", 250L)
                    ),
                memberships =
                    listOf(
                        GroupMembershipEntity(
                            membershipId = "membership-2",
                            sourceInvitationId = "invite-2",
                            groupId = GROUP_ID,
                            contactId = "admin-1",
                            perspective = "MEMBER",
                            status = "STAGED",
                            challenge = byteArrayOf(1),
                            createdAtEpochMilliseconds = 300L,
                            updatedAtEpochMilliseconds = 300L
                        )
                    )
            )

        assertFalse(timeline.isLocallyInactive)
        assertEquals(listOf("before", "You left this group"), timeline.visibleMessages.map(MessageEntity::text))
        assertEquals(listOf("invite-2"), timeline.currentMemberships.map(GroupMembershipEntity::sourceInvitationId))
    }

    @Test
    fun leavingWithoutAReinviteMakesConversationReadOnly() {
        val timeline =
            buildGroupLocalMembershipTimeline(
                messages =
                    listOf(
                        userMessage("before", 100L),
                        GroupMembershipMessageFactory.localMembershipLeft(
                            conversationId = GROUP_ID,
                            invitationId = "invite-1",
                            epoch = 2,
                            createdAtEpochMilliseconds = 200L
                        )
                    ),
                memberships = emptyList()
            )

        assertTrue(timeline.isLocallyInactive)
    }

    private fun userMessage(text: String, timestamp: Long): MessageEntity =
        MessageEntity(
            id = text,
            conversationId = GROUP_ID,
            packetId = "packet-$text",
            text = text,
            transportPayload = null,
            transportMode = "GROUP_E2EE",
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
            senderContactId = "contact-1",
            isMine = false,
            createdAtEpochMilliseconds = timestamp
        )

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
