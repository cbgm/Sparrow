package com.cbgm.sparrow.feature.membership.domain.model

data class GroupMemberProgress(
    val contactId: String,
    val status: GroupMemberProgressStatus
)

enum class GroupMemberProgressStatus {
    PENDING,
    JOINING,
    KEY_EXCHANGE,
    ACTIVE,
    FAILED
}
