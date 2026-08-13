package com.cbgm.securechat.feature.chats.data.group.mapper

import com.cbgm.securechat.feature.chats.domain.model.group.ChatMessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GroupMembershipMessageFactoryTest {
    @Test
    fun memberAddedMapsToTheReadOnlyMembershipEvent() {
        val message =
            GroupMembershipMessageFactory.memberAdded(
                conversationId = "group-1",
                epoch = 2,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 200L
            )

        assertEquals("Alex was added to the group", message.text)
        assertEquals(
            ChatMessageType.GROUP_MEMBER_ADDED,
            GroupMembershipMessageFactory.typeOf(message.transportMode)
        )
    }

    @Test
    fun repeatedRemovalUsesTheInvitationAsItsEventIdentity() {
        val first =
            GroupMembershipMessageFactory.memberRemoved(
                conversationId = "group-1",
                epoch = 0,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 100L,
                eventId = "invitation-1"
            )
        val second =
            GroupMembershipMessageFactory.memberRemoved(
                conversationId = "group-1",
                epoch = 0,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 200L,
                eventId = "invitation-2"
            )

        assertNotEquals(first.id, second.id)
        assertEquals(
            ChatMessageType.GROUP_MEMBER_REMOVED,
            GroupMembershipMessageFactory.typeOf(first.transportMode)
        )
    }

    @Test
    fun localRemovalMapsToTheReadOnlyMembershipEvent() {
        val message =
            GroupMembershipMessageFactory.localMembershipRemoved(
                conversationId = "group-1",
                invitationId = "invitation-1",
                epoch = 3,
                createdAtEpochMilliseconds = 300L
            )

        assertEquals(
            ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED,
            GroupMembershipMessageFactory.typeOf(message.transportMode)
        )
    }

    @Test
    fun voluntaryLeaveUsesDistinctTimelineEvents() {
        val ownerMessage =
            GroupMembershipMessageFactory.memberLeft(
                conversationId = "group-1",
                epoch = 4,
                contactId = "contact-1",
                contactName = "Alex",
                createdAtEpochMilliseconds = 400L
            )
        val localMessage =
            GroupMembershipMessageFactory.localMembershipLeft(
                conversationId = "group-1",
                invitationId = "invitation-1",
                epoch = 4,
                createdAtEpochMilliseconds = 400L
            )

        assertEquals(
            ChatMessageType.GROUP_MEMBER_LEFT,
            GroupMembershipMessageFactory.typeOf(ownerMessage.transportMode)
        )
        assertEquals(
            ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT,
            GroupMembershipMessageFactory.typeOf(localMessage.transportMode)
        )
    }

    @Test
    fun localConversationDeletionMarkerIsHiddenControlState() {
        val marker =
            GroupMembershipMessageFactory.localConversationDeletedMarker(
                conversationId = "group-1",
                createdAtEpochMilliseconds = 500L
            )

        assertEquals("", marker.text)
        assertEquals(
            GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE,
            marker.transportMode
        )
    }
}
