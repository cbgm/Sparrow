package com.cbgm.sparrow.feature.membership.data

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberProgress
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberProgressStatus

enum class GroupMembershipEvent {
    JOIN_REQUESTED,
    JOIN_SEND_FAILED,
    IDENTITY_CONFIRMED,
    WELCOME_SENT,
    WELCOME_RECEIVED,
    MEMBER_READY,
    MEMBER_ACTIVATED,
    LEAVE_REQUESTED,
    REMOVE,
    GROUP_DELETED
}

/**
 * Single source of truth for the membership/security lifecycle.
 *
 * Invitation decisions are deliberately not represented here. A pending, accepted,
 * declined or expired invitation is owned by feature:invite. A membership row only
 * tracks the security/join attempt that can eventually become an active membership.
 */
object GroupMembershipStateMachine {
    fun transition(
        currentStatus: String,
        event: GroupMembershipEvent
    ): GroupMembershipStatus {
        val current = currentStatus.toGroupMembershipStatus()
        val next = nextStatus(current, event)
        check(next != null) {
            "Unsupported group membership transition: $current + $event"
        }
        return next
    }

    fun conversationState(
        memberships: List<GroupMembershipEntity>,
        isLocallyInactive: Boolean = false
    ): GroupConversationState {
        if (memberships.hasStatus(GroupMembershipStatus.GROUP_DELETED)) {
            return GroupConversationState.DELETED
        }

        val currentMemberships = memberships.filterCurrentHistory()
        if (isLocallyInactive && currentMemberships.isEmpty()) {
            return GroupConversationState.REMOVED
        }
        if (currentMemberships.isEmpty() || currentMemberships.allHaveStatus(GroupMembershipStatus.ACTIVE)) {
            return GroupConversationState.READY
        }

        return currentMemberships.deriveConversationState()
    }

    fun leaveRequirement(
        isLocalAdmin: Boolean,
        currentMemberContactIds: Set<String>,
        currentAdminContactIds: Set<String>
    ): GroupLeaveRequirement =
        if (
            !isLocalAdmin ||
            currentMemberContactIds.isEmpty() ||
            currentAdminContactIds.isNotEmpty()
        ) {
            GroupLeaveRequirement.CanLeave
        } else {
            GroupLeaveRequirement.PromoteAdminFirst(currentMemberContactIds)
        }

    fun memberProgress(memberships: List<GroupMembershipEntity>): List<GroupMemberProgress> =
        memberships
            .filter { membership -> membership.shouldExposeProgress() }
            .map { membership ->
                GroupMemberProgress(
                    contactId = membership.contactId,
                    status = membership.status.toGroupMemberProgressStatus()
                )
            }

    private fun nextStatus(
        current: GroupMembershipStatus,
        event: GroupMembershipEvent
    ): GroupMembershipStatus? =
        when (event) {
            GroupMembershipEvent.JOIN_REQUESTED ->
                GroupMembershipStatus.JOIN_REQUEST_SENT.takeIf {
                    current == GroupMembershipStatus.STAGED
                }

            GroupMembershipEvent.JOIN_SEND_FAILED ->
                GroupMembershipStatus.FAILED.takeIf {
                    current == GroupMembershipStatus.JOIN_REQUEST_SENT
                }

            GroupMembershipEvent.IDENTITY_CONFIRMED ->
                GroupMembershipStatus.IDENTITY_READY.takeIf {
                    current == GroupMembershipStatus.STAGED
                }

            GroupMembershipEvent.WELCOME_SENT ->
                GroupMembershipStatus.WELCOME_SENT.takeIf {
                    current == GroupMembershipStatus.IDENTITY_READY
                }

            GroupMembershipEvent.WELCOME_RECEIVED ->
                GroupMembershipStatus.WAITING_FOR_ACTIVATION.takeIf {
                    current == GroupMembershipStatus.JOIN_REQUEST_SENT
                }

            GroupMembershipEvent.MEMBER_READY ->
                GroupMembershipStatus.ACTIVE.takeIf {
                    current == GroupMembershipStatus.WELCOME_SENT
                }

            GroupMembershipEvent.MEMBER_ACTIVATED ->
                GroupMembershipStatus.ACTIVE.takeIf {
                    current == GroupMembershipStatus.WAITING_FOR_ACTIVATION
                }

            GroupMembershipEvent.LEAVE_REQUESTED ->
                GroupMembershipStatus.LEAVE_REQUESTED.takeIf {
                    current !in TERMINAL_STATUSES
                }

            GroupMembershipEvent.REMOVE ->
                GroupMembershipStatus.REMOVED.takeIf {
                    current != GroupMembershipStatus.REMOVED &&
                        current != GroupMembershipStatus.GROUP_DELETED
                }

            GroupMembershipEvent.GROUP_DELETED ->
                GroupMembershipStatus.GROUP_DELETED.takeIf {
                    current != GroupMembershipStatus.GROUP_DELETED
                }
        }

    private fun List<GroupMembershipEntity>.deriveConversationState(): GroupConversationState =
        when {
            hasStatus(GroupMembershipStatus.LEAVE_REQUESTED) -> GroupConversationState.LEAVING
            hasStatus(GroupMembershipStatus.JOIN_REQUEST_SENT) ||
                hasStatus(GroupMembershipStatus.WAITING_FOR_ACTIVATION) -> GroupConversationState.JOINING
            hasStatus(GroupMembershipStatus.ACTIVE) -> GroupConversationState.READY
            hasStatus(GroupMembershipStatus.IDENTITY_READY) ||
                hasStatus(GroupMembershipStatus.WELCOME_SENT) -> GroupConversationState.DISTRIBUTING_KEYS
            hasStatus(GroupMembershipStatus.STAGED) -> GroupConversationState.WAITING_FOR_MEMBERS
            hasStatus(GroupMembershipStatus.FAILED) -> GroupConversationState.FAILED
            else -> GroupConversationState.WAITING_FOR_MEMBERS
        }

    private fun List<GroupMembershipEntity>.filterCurrentHistory(): List<GroupMembershipEntity> =
        filterNot { membership -> membership.status == GroupMembershipStatus.REMOVED.name }

    private fun List<GroupMembershipEntity>.hasStatus(status: GroupMembershipStatus): Boolean =
        any { membership -> membership.status == status.name }

    private fun List<GroupMembershipEntity>.allHaveStatus(status: GroupMembershipStatus): Boolean =
        all { membership -> membership.status == status.name }

    private fun String.toGroupMembershipStatus(): GroupMembershipStatus =
        GroupMembershipStatus.entries.firstOrNull { status -> status.name == this }
            ?: error("Unknown group membership status: $this")

    private fun GroupMembershipEntity.shouldExposeProgress(): Boolean =
        status != GroupMembershipStatus.REMOVED.name &&
            status != GroupMembershipStatus.GROUP_DELETED.name

    private fun String.toGroupMemberProgressStatus(): GroupMemberProgressStatus =
        when (this) {
            GroupMembershipStatus.STAGED.name -> GroupMemberProgressStatus.PENDING
            GroupMembershipStatus.IDENTITY_READY.name,
            GroupMembershipStatus.JOIN_REQUEST_SENT.name -> GroupMemberProgressStatus.JOINING
            GroupMembershipStatus.WELCOME_SENT.name,
            GroupMembershipStatus.WAITING_FOR_ACTIVATION.name -> GroupMemberProgressStatus.KEY_EXCHANGE
            GroupMembershipStatus.ACTIVE.name -> GroupMemberProgressStatus.ACTIVE
            GroupMembershipStatus.FAILED.name -> GroupMemberProgressStatus.FAILED
            else -> GroupMemberProgressStatus.PENDING
        }

    private val TERMINAL_STATUSES =
        setOf(
            GroupMembershipStatus.FAILED,
            GroupMembershipStatus.REMOVED,
            GroupMembershipStatus.GROUP_DELETED
        )
}
