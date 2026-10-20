package com.cbgm.sparrow.feature.chats.data.group.mapper

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberLifecycleSnapshot
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
                        localMembershipLeft(invitationId = "invite-1", epoch = 2, createdAtEpochMilliseconds = 200L),
                        userMessage("absent", 300L),
                        localMembershipStarted(referenceId = "invite-2", epoch = 4, createdAtEpochMilliseconds = 400L),
                        userMessage("after", 500L)
                    ),
                memberships = emptyList()
            )

        assertEquals(
            listOf("before", "group-local-membership-left-invite-1-2", "after"),
            timeline.visibleMessages.map(MessageEntity::id)
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
                        localMembershipLeft(invitationId = "invite-1", epoch = 2, createdAtEpochMilliseconds = 200L),
                        userMessage("absent", 250L)
                    ),
                memberships =
                    listOf(
                        GroupMemberLifecycleSnapshot(
                            sourceInvitationId = "invite-2",
                            contactId = "admin-1",
                            status = "STAGED",
                            createdAtEpochMilliseconds = 300L
                        )
                    )
            )

        assertFalse(timeline.isLocallyInactive)
        assertEquals(
            listOf("before", "group-local-membership-left-invite-1-2"),
            timeline.visibleMessages.map(MessageEntity::id)
        )
        assertEquals(
            listOf("invite-2"),
            timeline.currentMemberships.map(GroupMemberLifecycleSnapshot::sourceInvitationId)
        )
    }

    @Test
    fun leavingWithoutAReinviteMakesConversationReadOnly() {
        val timeline =
            buildGroupLocalMembershipTimeline(
                messages =
                    listOf(
                        userMessage("before", 100L),
                        localMembershipLeft(invitationId = "invite-1", epoch = 2, createdAtEpochMilliseconds = 200L)
                    ),
                memberships = emptyList()
            )

        assertTrue(timeline.isLocallyInactive)
    }

    private fun userMessage(id: String, timestamp: Long): MessageEntity =
        MessageEntity(
            id = id,
            conversationId = GROUP_ID,
            packetId = "packet-$id",
            transportPayload = null,
            transportMode = "GROUP_E2EE",
            contentStatus = MessageContentStatus.READABLE.name,
            deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE.name,
            senderContactId = "contact-1",
            isMine = false,
            createdAtEpochMilliseconds = timestamp
        )

    private fun localMembershipLeft(
        invitationId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): MessageEntity =
        localMarker(
            id = "group-local-membership-left-$invitationId-$epoch",
            transportMode = MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT,
            timestamp = createdAtEpochMilliseconds
        )

    private fun localMembershipStarted(
        referenceId: String,
        epoch: Int,
        createdAtEpochMilliseconds: Long
    ): MessageEntity =
        localMarker(
            id = "group-local-membership-started-$referenceId-$epoch",
            transportMode = MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED,
            timestamp = createdAtEpochMilliseconds
        )

    private fun localMarker(id: String, transportMode: String, timestamp: Long): MessageEntity =
        userMessage(id, timestamp).copy(
            packetId = null,
            transportMode = transportMode,
            senderContactId = null,
            isMine = true
        )

    private companion object {
        const val GROUP_ID = "group-1"
    }
}
