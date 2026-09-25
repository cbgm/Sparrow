package com.cbgm.sparrow.feature.membership.domain.model

enum class MembershipStatus {
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

enum class MembershipPerspective {
    OWNER,
    MEMBER
}
