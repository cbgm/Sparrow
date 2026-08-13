package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.data.group.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.group.GroupMemberInvitationState
import com.cbgm.securechat.feature.chats.domain.model.group.GroupMemberInvitationStatus

internal enum class GroupMembershipEvent {
    ACCEPT,
    EXPIRE,
    JOIN_SEND_FAILED,
    IDENTITY_CONFIRMED,
    WELCOME_SENT,
    WELCOME_RECEIVED,
    MEMBER_READY,
    MEMBER_ACTIVATED,
    DECLINE,
    LEAVE_REQUESTED,
    REMOVE,
    GROUP_DELETED
}

/**
 * Single source of truth for the group membership lifecycle.
 *
 * Incoming handlers and coordinators express *events*. This object decides the
 * next persisted invitation status and also derives the user-visible group state.
 */
internal object GroupMembershipStateMachine {
    fun transition(
        currentStatus: String,
        event: GroupMembershipEvent
    ): GroupInvitationStatus {
        val current = currentStatus.toInvitationStatus()
        val next = nextStatus(current, event)
        check(next != null) {
            "Unsupported group membership transition: $current + $event"
        }
        return next
    }

    fun conversationState(
        invitations: List<GroupInvitationEntity>,
        hasLocalMembershipRemoval: Boolean = false
    ): GroupConversationState {
        if (invitations.hasStatus(GroupInvitationStatus.GROUP_DELETED)) {
            return GroupConversationState.DELETED
        }

        val currentInvitations = invitations.filterActiveHistory()
        if (hasLocalMembershipRemoval && currentInvitations.isEmpty() && invitations.hasStatus(GroupInvitationStatus.REMOVED)) {
            return GroupConversationState.REMOVED
        }
        if (currentInvitations.isEmpty() || currentInvitations.allHaveStatus(GroupInvitationStatus.ACTIVE)) {
            return GroupConversationState.READY
        }

        return currentInvitations.deriveConversationState()
    }

    fun isIncoming(invitations: List<GroupInvitationEntity>): Boolean =
        invitations.any { invitation -> invitation.status.isIncomingMembershipStatus() }

    fun memberStates(invitations: List<GroupInvitationEntity>): List<GroupMemberInvitationState> =
        invitations
            .filterNot { invitation -> invitation.status.isHiddenMemberStatus() }
            .map { invitation ->
                GroupMemberInvitationState(
                    contactId = invitation.contactId,
                    status = invitation.status.toMemberStatus()
                )
            }

    private fun nextStatus(
        current: GroupInvitationStatus,
        event: GroupMembershipEvent
    ): GroupInvitationStatus? =
        when (event) {
            GroupMembershipEvent.ACCEPT ->
                GroupInvitationStatus.JOIN_SENT.takeIf {
                    current == GroupInvitationStatus.AWAITING_ACCEPTANCE
                }

            GroupMembershipEvent.EXPIRE ->
                GroupInvitationStatus.EXPIRED.takeIf {
                    current == GroupInvitationStatus.AWAITING_ACCEPTANCE
                }

            GroupMembershipEvent.JOIN_SEND_FAILED ->
                GroupInvitationStatus.FAILED.takeIf {
                    current == GroupInvitationStatus.JOIN_SENT
                }

            GroupMembershipEvent.IDENTITY_CONFIRMED ->
                GroupInvitationStatus.IDENTITY_READY.takeIf {
                    current == GroupInvitationStatus.INVITE_SENT ||
                        current == GroupInvitationStatus.WAITING_FOR_IDENTITY
                }

            GroupMembershipEvent.WELCOME_SENT ->
                GroupInvitationStatus.WELCOME_SENT.takeIf {
                    current == GroupInvitationStatus.IDENTITY_READY
                }

            GroupMembershipEvent.WELCOME_RECEIVED ->
                GroupInvitationStatus.WAITING_FOR_ACTIVATION.takeIf {
                    current == GroupInvitationStatus.JOIN_SENT
                }

            GroupMembershipEvent.MEMBER_READY ->
                GroupInvitationStatus.ACTIVE.takeIf {
                    current == GroupInvitationStatus.WELCOME_SENT
                }

            GroupMembershipEvent.MEMBER_ACTIVATED ->
                GroupInvitationStatus.ACTIVE.takeIf {
                    current == GroupInvitationStatus.WAITING_FOR_ACTIVATION
                }

            GroupMembershipEvent.DECLINE ->
                GroupInvitationStatus.DECLINED.takeIf {
                    current in DECLINABLE_STATUSES
                }

            GroupMembershipEvent.LEAVE_REQUESTED ->
                GroupInvitationStatus.LEAVE_SENT.takeIf {
                    current !in TERMINAL_STATUSES
                }

            GroupMembershipEvent.REMOVE ->
                GroupInvitationStatus.REMOVED.takeIf {
                    current != GroupInvitationStatus.REMOVED &&
                        current != GroupInvitationStatus.GROUP_DELETED
                }

            GroupMembershipEvent.GROUP_DELETED ->
                GroupInvitationStatus.GROUP_DELETED.takeIf {
                    current != GroupInvitationStatus.GROUP_DELETED
                }
        }

    private fun List<GroupInvitationEntity>.deriveConversationState(): GroupConversationState =
        when {
            hasStatus(GroupInvitationStatus.AWAITING_ACCEPTANCE) -> GroupConversationState.INVITED
            hasStatus(GroupInvitationStatus.LEAVE_SENT) -> GroupConversationState.LEAVING
            hasStatus(GroupInvitationStatus.JOIN_SENT) ||
                hasStatus(GroupInvitationStatus.WAITING_FOR_ACTIVATION) -> GroupConversationState.JOINING
            hasStatus(GroupInvitationStatus.ACTIVE) -> GroupConversationState.READY
            hasStatus(GroupInvitationStatus.WELCOME_SENT) -> GroupConversationState.DISTRIBUTING_KEYS
            hasStatus(GroupInvitationStatus.DECLINED) -> GroupConversationState.DECLINED
            hasStatus(GroupInvitationStatus.EXPIRED) -> GroupConversationState.EXPIRED
            hasStatus(GroupInvitationStatus.FAILED) -> GroupConversationState.FAILED
            else -> GroupConversationState.WAITING_FOR_MEMBERS
        }

    private fun List<GroupInvitationEntity>.filterActiveHistory(): List<GroupInvitationEntity> =
        filterNot { invitation -> invitation.status == GroupInvitationStatus.REMOVED.name }

    private fun List<GroupInvitationEntity>.hasStatus(status: GroupInvitationStatus): Boolean =
        any { invitation -> invitation.status == status.name }

    private fun List<GroupInvitationEntity>.allHaveStatus(status: GroupInvitationStatus): Boolean =
        all { invitation -> invitation.status == status.name }

    private fun String.toInvitationStatus(): GroupInvitationStatus =
        GroupInvitationStatus.entries.firstOrNull { status -> status.name == this }
            ?: error("Unknown group invitation status: $this")

    private fun String.isIncomingMembershipStatus(): Boolean =
        this == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
            this == GroupInvitationStatus.JOIN_SENT.name ||
            this == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name ||
            this == GroupInvitationStatus.LEAVE_SENT.name

    private fun String.isHiddenMemberStatus(): Boolean =
        this == GroupInvitationStatus.REMOVED.name ||
            this == GroupInvitationStatus.GROUP_DELETED.name

    private fun String.toMemberStatus(): GroupMemberInvitationStatus =
        when (this) {
            GroupInvitationStatus.IDENTITY_READY.name -> GroupMemberInvitationStatus.ACCEPTED
            GroupInvitationStatus.WELCOME_SENT.name,
            GroupInvitationStatus.WAITING_FOR_ACTIVATION.name -> GroupMemberInvitationStatus.KEY_SENT
            GroupInvitationStatus.ACTIVE.name -> GroupMemberInvitationStatus.ACTIVE
            GroupInvitationStatus.DECLINED.name -> GroupMemberInvitationStatus.DECLINED
            GroupInvitationStatus.EXPIRED.name -> GroupMemberInvitationStatus.EXPIRED
            GroupInvitationStatus.FAILED.name -> GroupMemberInvitationStatus.FAILED
            else -> GroupMemberInvitationStatus.INVITED
        }

    private val DECLINABLE_STATUSES =
        setOf(
            GroupInvitationStatus.AWAITING_ACCEPTANCE,
            GroupInvitationStatus.INVITE_SENT,
            GroupInvitationStatus.WAITING_FOR_IDENTITY,
            GroupInvitationStatus.IDENTITY_READY
        )

    private val TERMINAL_STATUSES =
        setOf(
            GroupInvitationStatus.DECLINED,
            GroupInvitationStatus.EXPIRED,
            GroupInvitationStatus.FAILED,
            GroupInvitationStatus.REMOVED,
            GroupInvitationStatus.GROUP_DELETED
        )
}
