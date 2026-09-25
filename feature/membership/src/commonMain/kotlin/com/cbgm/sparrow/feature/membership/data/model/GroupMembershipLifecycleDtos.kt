package com.cbgm.sparrow.feature.membership.data.model

enum class GroupConversationStateDto {
    READY,
    JOINING,
    WAITING_FOR_MEMBERS,
    DISTRIBUTING_KEYS,
    LEAVING,
    REMOVED,
    DELETED,
    FAILED
}

sealed interface GroupLeaveRequirementDto {
    data object CanLeave : GroupLeaveRequirementDto

    data class PromoteAdminFirst(
        val contactIds: Set<String>
    ) : GroupLeaveRequirementDto
}

data class GroupMemberProgressDto(
    val contactId: String,
    val status: GroupMemberProgressStatusDto
)

enum class GroupMemberProgressStatusDto {
    PENDING,
    JOINING,
    KEY_EXCHANGE,
    ACTIVE,
    FAILED
}
