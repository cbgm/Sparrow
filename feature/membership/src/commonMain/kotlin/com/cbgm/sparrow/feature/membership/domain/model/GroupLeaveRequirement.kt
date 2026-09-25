package com.cbgm.sparrow.feature.membership.domain.model

sealed interface GroupLeaveRequirement {
    data object CanLeave : GroupLeaveRequirement

    data class PromoteAdminFirst(
        val contactIds: Set<String>
    ) : GroupLeaveRequirement
}
