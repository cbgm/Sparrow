package com.cbgm.sparrow.feature.membership.data

import com.cbgm.sparrow.data.database.entity.GroupMembershipEntity
import com.cbgm.sparrow.feature.membership.data.model.GroupConversationStateDto
import com.cbgm.sparrow.feature.membership.data.model.GroupLeaveRequirementDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMemberProgressDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMemberProgressStatusDto
import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationMembershipProjector
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberLifecycleSnapshot

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
    ): GroupConversationStateDto =
        GroupConversationStateDto.valueOf(
            GroupConversationMembershipProjector.project(
                memberships.map { membership -> membership.toLifecycleSnapshot() },
                isLocallyInactive
            ).state.name
        )

    fun leaveRequirement(
        isLocalAdmin: Boolean,
        currentMemberContactIds: Set<String>,
        currentAdminContactIds: Set<String>
    ): GroupLeaveRequirementDto =
        if (
            !isLocalAdmin ||
            currentMemberContactIds.isEmpty() ||
            currentAdminContactIds.isNotEmpty()
        ) {
            GroupLeaveRequirementDto.CanLeave
        } else {
            GroupLeaveRequirementDto.PromoteAdminFirst(currentMemberContactIds)
        }

    fun memberProgress(memberships: List<GroupMembershipEntity>): List<GroupMemberProgressDto> =
        GroupConversationMembershipProjector
            .project(memberships.map { membership -> membership.toLifecycleSnapshot() }, isLocallyInactive = false)
            .memberProgress
            .map { progress ->
                GroupMemberProgressDto(
                    contactId = progress.contactId,
                    status = GroupMemberProgressStatusDto.valueOf(progress.status.name)
                )
            }

    private fun GroupMembershipEntity.toLifecycleSnapshot(): GroupMemberLifecycleSnapshot =
        GroupMemberLifecycleSnapshot(
            contactId = contactId,
            sourceInvitationId = sourceInvitationId,
            status = status,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )

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

    private fun String.toGroupMembershipStatus(): GroupMembershipStatus =
        GroupMembershipStatus.entries.firstOrNull { status -> status.name == this }
            ?: error("Unknown group membership status: $this")

    private val TERMINAL_STATUSES =
        setOf(
            GroupMembershipStatus.FAILED,
            GroupMembershipStatus.REMOVED,
            GroupMembershipStatus.GROUP_DELETED
        )
}
