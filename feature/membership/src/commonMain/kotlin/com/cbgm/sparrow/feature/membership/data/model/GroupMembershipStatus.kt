package com.cbgm.sparrow.feature.membership.data.model

enum class GroupMembershipStatus {
    STAGED,
    IDENTITY_READY,
    JOIN_REQUEST_SENT,
    WELCOME_SENT,
    WAITING_FOR_ACTIVATION,
    LEAVE_REQUESTED,
    FAILED,
    REMOVED,
    GROUP_DELETED,
    ACTIVE
}

enum class GroupMembershipPerspective {
    OWNER,
    MEMBER
}
