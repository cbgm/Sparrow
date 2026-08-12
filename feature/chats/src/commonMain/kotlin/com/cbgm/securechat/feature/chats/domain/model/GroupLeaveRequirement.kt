package com.cbgm.securechat.feature.chats.domain.model

sealed interface GroupLeaveRequirement {
    data object CanLeave : GroupLeaveRequirement

    data class PromoteAdminFirst(
        val contactIds: Set<String>
    ) : GroupLeaveRequirement
}
