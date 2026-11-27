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
    ACTIVE;

    /** Member side: the invitation was accepted and the join is underway or done. */
    val isJoinInProgress: Boolean
        get() = this == JOIN_REQUEST_SENT || this == WAITING_FOR_ACTIVATION || this == ACTIVE

    /** Owner side: the welcome went out, so an approved identity change has nothing left to resume. */
    val isWelcomeSent: Boolean
        get() = this == WELCOME_SENT || this == WAITING_FOR_ACTIVATION || this == ACTIVE

    /** Owner side: the join still waits for the member's identity before the welcome. */
    val awaitsMemberIdentity: Boolean
        get() = this == STAGED || this == IDENTITY_READY
}

enum class MembershipPerspective {
    OWNER,
    MEMBER
}
