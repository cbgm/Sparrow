package com.cbgm.sparrow.feature.membership.domain.model

data class GroupMembershipContext(
    val title: String,
    val createdAtEpochMilliseconds: Long
) {
    init {
        require(title.isNotBlank()) { "Group title must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Group creation timestamp must not be negative" }
    }
}
