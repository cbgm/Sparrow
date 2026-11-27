package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model

import com.cbgm.sparrow.feature.membership.domain.model.GroupCommittedMemberRemoval
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalReason
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import kotlin.test.Test
import kotlin.test.assertEquals

/** A retry of a removal continues where the earlier attempt stopped instead of failing on an inactive member. */
class GroupMemberRemovalProgressTest {
    @Test
    fun memberWithoutCommittedRemovalIsRemoved() {
        assertEquals(GroupMemberRemovalProgress.NotStarted, GroupMemberRemovalProgress.of(null, isParticipant = true))
    }

    @Test
    fun committedRemovalStillShownAsParticipantIsProjected() {
        val committed = committed(isRecorded = true)
        assertEquals(
            GroupMemberRemovalProgress.Committed(committed.result),
            GroupMemberRemovalProgress.of(committed, isParticipant = true)
        )
    }

    @Test
    fun memberOtherAdminInvitedIsProjectedFromTheParticipantRow() {
        val committed = committed(isRecorded = false)
        assertEquals(
            GroupMemberRemovalProgress.Committed(committed.result),
            GroupMemberRemovalProgress.of(committed, isParticipant = true)
        )
    }

    @Test
    fun projectedRemovalOnlyRefreshesVerification() {
        assertEquals(
            GroupMemberRemovalProgress.Projected,
            GroupMemberRemovalProgress.of(committed(isRecorded = true), isParticipant = false)
        )
    }

    @Test
    fun contactThatWasNeverAMemberIsStillRefusedByMembership() {
        assertEquals(
            GroupMemberRemovalProgress.NotStarted,
            GroupMemberRemovalProgress.of(committed(isRecorded = false), isParticipant = false)
        )
    }

    private fun committed(isRecorded: Boolean) =
        GroupCommittedMemberRemoval(
            result = GroupMemberRemovalResult(
                groupId = "group-1",
                contactId = "alice",
                epoch = 3,
                eventId = "invitation-1",
                updatedAtEpochMilliseconds = 10L,
                reason = GroupMemberRemovalReason.REMOVED
            ),
            isRecorded = isRecorded
        )
}
