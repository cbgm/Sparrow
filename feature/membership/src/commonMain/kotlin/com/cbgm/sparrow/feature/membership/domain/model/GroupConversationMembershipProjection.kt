package com.cbgm.sparrow.feature.membership.domain.model

/** A pure membership-state projection; does not read Chats' messages or databases. */
data class GroupConversationMembershipProjection(
    val state: GroupConversationState,
    val memberProgress: List<GroupMemberProgress>,
    val pendingMemberCount: Int
)

object GroupConversationMembershipProjector {
    fun project(
        memberships: List<GroupMemberLifecycleSnapshot>,
        isLocallyInactive: Boolean
    ): GroupConversationMembershipProjection {
        val statuses = memberships.map(GroupMemberLifecycleSnapshot::status)
        val currentStatuses = statuses.filterNot { it == REMOVED }
        val state = when {
            DELETED in statuses -> GroupConversationState.DELETED
            isLocallyInactive && currentStatuses.isEmpty() -> GroupConversationState.REMOVED
            currentStatuses.isEmpty() || currentStatuses.all { it == ACTIVE } -> GroupConversationState.READY
            LEAVING in currentStatuses -> GroupConversationState.LEAVING
            JOIN_REQUEST_SENT in currentStatuses || WAITING_FOR_ACTIVATION in currentStatuses -> GroupConversationState.JOINING
            ACTIVE in currentStatuses -> GroupConversationState.READY
            IDENTITY_READY in currentStatuses || WELCOME_SENT in currentStatuses -> GroupConversationState.DISTRIBUTING_KEYS
            STAGED in currentStatuses -> GroupConversationState.WAITING_FOR_MEMBERS
            FAILED in currentStatuses -> GroupConversationState.FAILED
            else -> GroupConversationState.WAITING_FOR_MEMBERS
        }
        val progress = memberships
            .filterNot { it.status == REMOVED || it.status == DELETED }
            .map { membership ->
                GroupMemberProgress(
                    contactId = membership.contactId,
                    status = when (membership.status) {
                        STAGED -> GroupMemberProgressStatus.PENDING
                        IDENTITY_READY, JOIN_REQUEST_SENT -> GroupMemberProgressStatus.JOINING
                        WELCOME_SENT, WAITING_FOR_ACTIVATION -> GroupMemberProgressStatus.KEY_EXCHANGE
                        ACTIVE -> GroupMemberProgressStatus.ACTIVE
                        FAILED -> GroupMemberProgressStatus.FAILED
                        else -> GroupMemberProgressStatus.PENDING
                    }
                )
            }
        return GroupConversationMembershipProjection(
            state = state,
            memberProgress = progress,
            pendingMemberCount = statuses.count { it in PENDING_STATUSES }
        )
    }

    private const val STAGED = "STAGED"
    private const val IDENTITY_READY = "IDENTITY_READY"
    private const val JOIN_REQUEST_SENT = "JOIN_REQUEST_SENT"
    private const val WELCOME_SENT = "WELCOME_SENT"
    private const val WAITING_FOR_ACTIVATION = "WAITING_FOR_ACTIVATION"
    private const val ACTIVE = "ACTIVE"
    private const val LEAVING = "LEAVE_REQUESTED"
    private const val REMOVED = "REMOVED"
    private const val DELETED = "GROUP_DELETED"
    private const val FAILED = "FAILED"
    private val PENDING_STATUSES = setOf(STAGED, IDENTITY_READY, JOIN_REQUEST_SENT, WELCOME_SENT, WAITING_FOR_ACTIVATION)
}
