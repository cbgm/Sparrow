package com.cbgm.sparrow.feature.chats.data.group.membership

import com.cbgm.sparrow.data.database.entity.GroupInvitationEntity
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationDirection
import com.cbgm.sparrow.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMemberInvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupMembershipStateMachineTest {
    @Test
    fun recipientInvitationRequiresAcceptanceBeforeJoining() {
        val invited = listOf(invitation(GroupInvitationStatus.AWAITING_ACCEPTANCE))
        val joining = listOf(invitation(GroupInvitationStatus.JOIN_SENT))

        assertEquals(GroupConversationState.INVITED, GroupMembershipStateMachine.conversationState(invited))
        assertEquals(GroupConversationState.JOINING, GroupMembershipStateMachine.conversationState(joining))
        assertTrue(GroupMembershipStateMachine.isIncoming(invited))
        assertTrue(GroupMembershipStateMachine.isIncoming(joining))
    }

    @Test
    fun creatorIsReadyWhenAtLeastOneMemberIsActive() {
        val partiallyActive =
            listOf(
                invitation(GroupInvitationStatus.ACTIVE, contactId = "contact-1"),
                invitation(GroupInvitationStatus.WELCOME_SENT, contactId = "contact-2")
            )

        val fullyActive =
            listOf(
                invitation(GroupInvitationStatus.ACTIVE, contactId = "contact-1"),
                invitation(GroupInvitationStatus.ACTIVE, contactId = "contact-2")
            )

        assertEquals(
            GroupConversationState.READY,
            GroupMembershipStateMachine.conversationState(partiallyActive)
        )

        assertEquals(
            GroupConversationState.READY,
            GroupMembershipStateMachine.conversationState(fullyActive)
        )
    }

    @Test
    fun declinedInvitationRemainsVisibleAndBlocksActivation() {
        val declined = listOf(invitation(GroupInvitationStatus.DECLINED))
        val invitations =
            listOf(
                invitation(GroupInvitationStatus.IDENTITY_READY, contactId = "contact-1"),
                invitation(GroupInvitationStatus.DECLINED, contactId = "contact-2")
            )

        assertEquals(
            GroupConversationState.DECLINED,
            GroupMembershipStateMachine.conversationState(declined)
        )
        assertFalse(GroupMembershipStateMachine.isIncoming(declined))
        assertEquals(
            GroupConversationState.DECLINED,
            GroupMembershipStateMachine.conversationState(invitations)
        )
        assertEquals(
            GroupMemberInvitationStatus.DECLINED,
            GroupMembershipStateMachine.memberStates(invitations)[1].status
        )
    }

    @Test
    fun removedMembersNoLongerAffectConversationOrMemberState() {
        val invitations =
            listOf(
                invitation(GroupInvitationStatus.ACTIVE, contactId = "contact-1"),
                invitation(GroupInvitationStatus.REMOVED, contactId = "contact-2")
            )

        assertEquals(
            GroupConversationState.READY,
            GroupMembershipStateMachine.conversationState(invitations)
        )
        assertEquals(
            listOf("contact-1"),
            GroupMembershipStateMachine.memberStates(invitations).map { member -> member.contactId }
        )
    }

    @Test
    fun localRemovalEventBlocksOnlyTheRemovedRecipient() {
        val removed = listOf(invitation(GroupInvitationStatus.REMOVED))

        assertEquals(
            GroupConversationState.REMOVED,
            GroupMembershipStateMachine.conversationState(
                invitations = removed,
                isLocallyInactive = true
            )
        )
        assertEquals(
            GroupConversationState.READY,
            GroupMembershipStateMachine.conversationState(
                invitations = removed,
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
            GroupLeaveRequirement.PromoteAdminFirst(setOf("member-1", "member-2")),
            requirement
        )
    }

    @Test
    fun adminCanLeaveWhenAnotherAdminRemainsOrGroupIsEmpty() {
        assertEquals(
            GroupLeaveRequirement.CanLeave,
            GroupMembershipStateMachine.leaveRequirement(
                isLocalAdmin = true,
                currentMemberContactIds = setOf("admin-2"),
                currentAdminContactIds = setOf("admin-2")
            )
        )
        assertEquals(
            GroupLeaveRequirement.CanLeave,
            GroupMembershipStateMachine.leaveRequirement(
                isLocalAdmin = true,
                currentMemberContactIds = emptySet(),
                currentAdminContactIds = emptySet()
            )
        )
    }

    @Test
    fun queuedLeaveRequestMakesTheRecipientReadOnly() {
        val leaving = listOf(invitation(GroupInvitationStatus.LEAVE_SENT))

        assertEquals(
            GroupConversationState.LEAVING,
            GroupMembershipStateMachine.conversationState(leaving)
        )
        assertTrue(GroupMembershipStateMachine.isIncoming(leaving))
    }

    @Test
    fun ownerDeletionKeepsHistoryReadOnly() {
        val deleted = listOf(invitation(GroupInvitationStatus.GROUP_DELETED))

        assertEquals(
            GroupConversationState.DELETED,
            GroupMembershipStateMachine.conversationState(deleted)
        )
        assertFalse(GroupMembershipStateMachine.isIncoming(deleted))
        assertTrue(GroupMembershipStateMachine.memberStates(deleted).isEmpty())
    }

    @Test
    fun incomingMembershipEventsFollowOneExplicitStatePath() {
        val joinSent =
            GroupMembershipStateMachine.transition(
                GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                GroupMembershipEvent.ACCEPT
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

        assertEquals(GroupInvitationStatus.JOIN_SENT, joinSent)
        assertEquals(GroupInvitationStatus.WAITING_FOR_ACTIVATION, waitingForActivation)
        assertEquals(GroupInvitationStatus.ACTIVE, active)
    }

    @Test
    fun outgoingMembershipEventsFollowOneExplicitStatePath() {
        val identityReady =
            GroupMembershipStateMachine.transition(
                GroupInvitationStatus.INVITE_SENT.name,
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

        assertEquals(GroupInvitationStatus.IDENTITY_READY, identityReady)
        assertEquals(GroupInvitationStatus.WELCOME_SENT, welcomeSent)
        assertEquals(GroupInvitationStatus.ACTIVE, active)
    }

    @Test
    fun outgoingInviteIsPendingOnlyAfterRecipientReceipt() {
        val received =
            GroupMembershipStateMachine.transition(
                GroupInvitationStatus.INVITE_SENT.name,
                GroupMembershipEvent.INVITE_RECEIVED
            )

        assertEquals(GroupInvitationStatus.INVITE_RECEIVED, received)
    }

    @Test
    fun failedInvitationSendBecomesRetryableTerminalState() {
        val failed =
            GroupMembershipStateMachine.transition(
                GroupInvitationStatus.INVITE_SENT.name,
                GroupMembershipEvent.INVITE_SEND_FAILED
            )

        assertEquals(GroupInvitationStatus.FAILED, failed)
    }

    @Test
    fun invalidMembershipTransitionFailsImmediately() {
        assertFailsWith<IllegalStateException> {
            GroupMembershipStateMachine.transition(
                GroupInvitationStatus.ACTIVE.name,
                GroupMembershipEvent.ACCEPT
            )
        }
    }

    private fun invitation(
        status: GroupInvitationStatus,
        contactId: String = "contact-1"
    ): GroupInvitationEntity =
        GroupInvitationEntity(
            invitationId = "invitation-$contactId",
            groupId = "group-1",
            contactId = contactId,
            direction = GroupInvitationDirection.OUTGOING.name,
            status = status.name,
            challenge = byteArrayOf(1),
            createdAtEpochMilliseconds = 100L,
            expiresAtEpochMilliseconds = 200L,
            updatedAtEpochMilliseconds = 100L
        )
}
