package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupDescription(
    val groupId: String,
    val description: String?,
    val changedAtEpochMilliseconds: Long
) {
    companion object {
        const val MAX_LENGTH = 500
    }
}
