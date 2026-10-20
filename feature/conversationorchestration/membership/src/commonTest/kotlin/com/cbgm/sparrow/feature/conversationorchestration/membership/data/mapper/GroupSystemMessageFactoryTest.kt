package com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper

import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GroupSystemMessageFactoryTest {
    @Test
    fun memberAddedMapsToTheReadOnlyMembershipEvent() {
        val message =
            GroupSystemMessageFactory.memberAdded(
                conversationId = "group-1",
                epoch = 2,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 200L
            )

        assertEquals("Alex was added to the group", message.text)
        assertEquals(
            MessageTransportMode.GROUP_MEMBER_ADDED,
            message.message.transportMode
        )
    }

    @Test
    fun repeatedRemovalUsesTheInvitationAsItsEventIdentity() {
        val first =
            GroupSystemMessageFactory.memberRemoved(
                conversationId = "group-1",
                epoch = 0,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 100L,
                eventId = "invitation-1"
            )
        val second =
            GroupSystemMessageFactory.memberRemoved(
                conversationId = "group-1",
                epoch = 0,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 200L,
                eventId = "invitation-2"
            )

        assertNotEquals(first.message.id, second.message.id)
        assertEquals(
            MessageTransportMode.GROUP_MEMBER_REMOVED,
            first.message.transportMode
        )
    }

    @Test
    fun localRemovalMapsToTheReadOnlyMembershipEvent() {
        val message =
            GroupSystemMessageFactory.localMembershipRemoved(
                conversationId = "group-1",
                invitationId = "invitation-1",
                epoch = 3,
                createdAtEpochMilliseconds = 300L
            )

        assertEquals(
            MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_REMOVED,
            message.message.transportMode
        )
    }

    @Test
    fun voluntaryLeaveUsesDistinctTimelineEvents() {
        val ownerMessage =
            GroupSystemMessageFactory.memberLeft(
                conversationId = "group-1",
                epoch = 4,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 400L
            )
        val localMessage =
            GroupSystemMessageFactory.localMembershipLeft(
                conversationId = "group-1",
                invitationId = "invitation-1",
                epoch = 4,
                createdAtEpochMilliseconds = 400L
            )

        assertEquals(
            MessageTransportMode.GROUP_MEMBER_LEFT,
            ownerMessage.message.transportMode
        )
        assertEquals(
            MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_LEFT,
            localMessage.message.transportMode
        )
    }

    @Test
    fun membershipRestartMarkerIsHiddenControlState() {
        val marker =
            GroupSystemMessageFactory.localMembershipStarted(
                conversationId = "group-1",
                referenceId = "invitation-2",
                epoch = 5,
                createdAtEpochMilliseconds = 500L
            )

        assertEquals("", marker.text)
        assertEquals(
            MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED,
            marker.message.transportMode
        )
    }

    @Test
    fun localConversationDeletionMarkerIsHiddenControlState() {
        val marker =
            GroupSystemMessageFactory.localConversationDeletedMarker(
                conversationId = "group-1",
                createdAtEpochMilliseconds = 500L
            )

        assertEquals("", marker.text)
        assertEquals(
            MessageTransportMode.LOCAL_CONVERSATION_DELETED,
            marker.message.transportMode
        )
    }
}
