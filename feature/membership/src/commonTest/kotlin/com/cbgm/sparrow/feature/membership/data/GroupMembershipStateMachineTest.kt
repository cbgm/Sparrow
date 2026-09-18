package com.cbgm.sparrow.feature.membership.data

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipPerspective
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.data.model.GroupConversationStateDto
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMemberProgressStatusDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupMembershipStateMachineTest {
    @Test
    fun stagedMemberWaitsWhileJoinRequestIsJoining() {
        val staged = listOf(membership(GroupMembershipStatus.STAGED))
        val joining = listOf(membership(GroupMembershipStatus.JOIN_REQUEST_SENT))

        assertEquals(
            GroupConversationStateDto.WAITING_FOR_MEMBERS,
            GroupMembershipStateMachine.conversationState(staged)
        )
        assertEquals(
            GroupConversationStateDto.JOINING,
            GroupMembershipStateMachine.conversationState(joining)
        )
    }

    @Test
    fun creatorIsReadyWhenAtLeastOneMemberIsActive() {
        val partiallyActive =
            listOf(
                membership(GroupMembershipStatus.ACTIVE, contactId = "contact-1"),
                membership(GroupMembershipStatus.WELCOME_SENT, contactId = "contact-2")
            )

        val fullyActive =
            listOf(
                membership(GroupMembershipStatus.ACTIVE, contactId = "contact-1"),
                membership(GroupMembershipStatus.ACTIVE, contactId = "contact-2")
            )

        assertEquals(
            GroupConversationStateDto.READY,
            GroupMembershipStateMachine.conversationState(partiallyActive)
        )
        assertEquals(
            GroupConversationStateDto.READY,
            GroupMembershipStateMachine.conversationState(fullyActive)
        )
    }

    @Test
    fun removedMembersNoLongerAffectConversationOrMemberProgress() {
        val memberships =
            listOf(
                membership(GroupMembershipStatus.ACTIVE, contactId = "contact-1"),
                membership(GroupMembershipStatus.REMOVED, contactId = "contact-2")
            )

        assertEquals(
            GroupConversationStateDto.READY,
            GroupMembershipStateMachine.conversationState(memberships)
        )
        assertEquals(
            listOf("contact-1"),
            GroupMembershipStateMachine.memberProgress(memberships).map { member -> member.contactId }
        )
    }

    @Test
    fun localRemovalEventBlocksOnlyTheRemovedRecipient() {
        val removed = listOf(membership(GroupMembershipStatus.REMOVED))

        assertEquals(
            GroupConversationStateDto.REMOVED,
            GroupMembershipStateMachine.conversationState(
                memberships = removed,
                isLocallyInactive = true
            )
        )
        assertEquals(
            GroupConversationStateDto.READY,
            GroupMembershipStateMachine.conversationState(
                memberships = removed,
                isLocallyInactive = false
            )
        )
    }

    @Test
    fun soleAdminMustPromoteWhenOtherMembersRemain() {
        val requirement =
            GroupMembershipStateMachine.leaveRequirement(
                isLocalAdmin = true,
                currentMemberContactIds = setOf("member-1", "member-2"),
                currentAdminContactIds = emptySet()
            )

        assertEquals(
            GroupLeaveRequirementDto.PromoteAdminFirst(setOf("member-1", "member-2")),
            requirement
        )
    }

    @Test
    fun adminCanLeaveWhenAnotherAdminRemainsOrGroupIsEmpty() {
        assertEquals(
            GroupLeaveRequirementDto.CanLeave,
            GroupMembershipStateMachine.leaveRequirement(
                isLocalAdmin = true,
                currentMemberContactIds = setOf("admin-2"),
                currentAdminContactIds = setOf("admin-2")
            )
        )
        assertEquals(
            GroupLeaveRequirementDto.CanLeave,
            GroupMembershipStateMachine.leaveRequirement(
                isLocalAdmin = true,
                currentMemberContactIds = emptySet(),
                currentAdminContactIds = emptySet()
            )
        )
    }

    @Test
    fun queuedLeaveRequestMakesConversationReadOnly() {
        val leaving = listOf(membership(GroupMembershipStatus.LEAVE_REQUESTED))

        assertEquals(
            GroupConversationStateDto.LEAVING,
            GroupMembershipStateMachine.conversationState(leaving)
        )
    }

    @Test
    fun ownerDeletionKeepsHistoryReadOnly() {
        val deleted = listOf(membership(GroupMembershipStatus.GROUP_DELETED))

        assertEquals(
            GroupConversationStateDto.DELETED,
            GroupMembershipStateMachine.conversationState(deleted)
        )
        assertEquals(emptyList(), GroupMembershipStateMachine.memberProgress(deleted))
    }

    @Test
    fun incomingMembershipEventsFollowOneExplicitStatePath() {
        val joinSent =
            GroupMembershipStateMachine.transition(
                GroupMembershipStatus.STAGED.name,
                GroupMembershipEvent.JOIN_REQUESTED
            )
        val waitingForActivation =
            GroupMembershipStateMachine.transition(
                joinSent.name,
                GroupMembershipEvent.WELCOME_RECEIVED
            )
        val active =
            GroupMembershipStateMachine.transition(
                waitingForActivation.name,
                GroupMembershipEvent.MEMBER_ACTIVATED
            )

        assertEquals(GroupMembershipStatus.JOIN_REQUEST_SENT, joinSent)
        assertEquals(GroupMembershipStatus.WAITING_FOR_ACTIVATION, waitingForActivation)
        assertEquals(GroupMembershipStatus.ACTIVE, active)
    }

    @Test
    fun outgoingMembershipEventsFollowOneExplicitStatePath() {
        val identityReady =
            GroupMembershipStateMachine.transition(
                GroupMembershipStatus.STAGED.name,
                GroupMembershipEvent.IDENTITY_CONFIRMED
            )
        val welcomeSent =
            GroupMembershipStateMachine.transition(
                identityReady.name,
                GroupMembershipEvent.WELCOME_SENT
            )
        val active =
            GroupMembershipStateMachine.transition(
                welcomeSent.name,
                GroupMembershipEvent.MEMBER_READY
            )

        assertEquals(GroupMembershipStatus.IDENTITY_READY, identityReady)
        assertEquals(GroupMembershipStatus.WELCOME_SENT, welcomeSent)
        assertEquals(GroupMembershipStatus.ACTIVE, active)
    }

    @Test
    fun stagedMembershipIsExposedAsPendingProgress() {
        val states =
            GroupMembershipStateMachine.memberProgress(
                listOf(membership(GroupMembershipStatus.STAGED))
            )

        assertEquals(1, states.size)
        assertEquals(GroupMemberProgressStatusDto.PENDING, states.single().status)
    }

    @Test
    fun joinSendFailureBecomesFailedMembership() {
        val joinSent =
            GroupMembershipStateMachine.transition(
                GroupMembershipStatus.STAGED.name,
                GroupMembershipEvent.JOIN_REQUESTED
            )
        val failed =
            GroupMembershipStateMachine.transition(
                joinSent.name,
                GroupMembershipEvent.JOIN_SEND_FAILED
            )

        assertEquals(GroupMembershipStatus.FAILED, failed)
    }

    @Test
    fun invalidMembershipTransitionFailsImmediately() {
        assertFailsWith<IllegalStateException> {
            GroupMembershipStateMachine.transition(
                GroupMembershipStatus.ACTIVE.name,
                GroupMembershipEvent.JOIN_REQUESTED
            )
        }
    }

    private fun membership(
        status: GroupMembershipStatus,
        contactId: String = "contact-1"
    ): GroupMembershipEntity =
        GroupMembershipEntity(
            membershipId = "membership-$contactId",
            sourceInvitationId = "invitation-$contactId",
            groupId = "group-1",
            contactId = contactId,
            perspective = GroupMembershipPerspective.OWNER.name,
            status = status.name,
            challenge = byteArrayOf(1),
            createdAtEpochMilliseconds = 100L,
            updatedAtEpochMilliseconds = 100L
        )
}
