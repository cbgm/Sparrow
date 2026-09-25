package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupAvatarMetadata(
    val groupId: String,
    val changedAtEpochMilliseconds: Long = 0L,
    val hasAvatar: Boolean = false
)
