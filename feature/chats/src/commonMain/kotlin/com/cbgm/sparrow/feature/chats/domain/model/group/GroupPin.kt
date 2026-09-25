package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupPin(
    val message: GroupMessage,
    val pinnedAtEpochMilliseconds: Long
) {
    init {
        require(pinnedAtEpochMilliseconds > 0L) { "Pinned-at timestamp must be positive" }
    }
}
