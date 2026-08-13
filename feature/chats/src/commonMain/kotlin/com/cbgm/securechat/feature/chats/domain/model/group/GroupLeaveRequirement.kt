package com.cbgm.securechat.feature.chats.domain.model.group

sealed interface GroupLeaveRequirement {
    data object CanLeave : GroupLeaveRequirement

    data class PromoteAdminFirst(
        val contactIds: Set<String>
    ) : GroupLeaveRequirement
}
