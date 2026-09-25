package com.cbgm.sparrow.feature.membership.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupConversationMembershipProjectorTest {
    @Test
    fun emptyMembershipsAreReadyUntilLocalMembershipEnds() {
        val initial = GroupConversationMembershipProjector.project(emptyList(), isLocallyInactive = false)
        val left = GroupConversationMembershipProjector.project(emptyList(), isLocallyInactive = true)
        assertEquals(GroupConversationState.READY, initial.state)
        assertEquals(GroupConversationState.REMOVED, left.state)
    }

    @Test
    fun deletedMembershipTakesPriorityOverAnActiveMember() {
        val snapshot = GroupConversationMembershipProjector.project(
            listOf(member("active", "ACTIVE"), member("deleted", "GROUP_DELETED")),
            isLocallyInactive = false
        )
        assertEquals(GroupConversationState.DELETED, snapshot.state)
        assertEquals(listOf("active"), snapshot.memberProgress.map(GroupMemberProgress::contactId))
    }

    @Test
    fun activeMemberMakesGroupReadyEvenWhileAnotherMemberIsJoining() {
        val snapshot = GroupConversationMembershipProjector.project(
            listOf(member("active", "ACTIVE"), member("joining", "JOIN_REQUEST_SENT")),
            isLocallyInactive = false
        )
        assertEquals(GroupConversationState.READY, snapshot.state)
        assertEquals(1, snapshot.pendingMemberCount)
        assertEquals(
            listOf(GroupMemberProgressStatus.ACTIVE, GroupMemberProgressStatus.JOINING),
            snapshot.memberProgress.map(GroupMemberProgress::status)
        )
    }

    @Test
    fun memberProgressAndPendingCountsRetainTheOriginalMembershipStatuses() {
        val snapshot = GroupConversationMembershipProjector.project(
            listOf(
                member("pending", "STAGED"),
                member("exchange", "WELCOME_SENT"),
                member("removed", "REMOVED"),
                member("failed", "FAILED")
            ),
            isLocallyInactive = false
        )
        assertEquals(GroupConversationState.DISTRIBUTING_KEYS, snapshot.state)
        assertEquals(2, snapshot.pendingMemberCount)
        assertEquals(
            listOf("pending", "exchange", "failed"),
            snapshot.memberProgress.map(GroupMemberProgress::contactId)
        )
    }

    private fun member(contactId: String, status: String): GroupMemberLifecycleSnapshot =
        GroupMemberLifecycleSnapshot(
            contactId = contactId,
            sourceInvitationId = "invite-$contactId",
            status = status,
            createdAtEpochMilliseconds = 1L
        )
}
