package com.cbgm.sparrow.feature.chats.domain.model.group

data class GroupAvatar(
    val groupId: String,
    val changedAtEpochMilliseconds: Long = 0L,
    val bytes: ByteArray? = null
) {
    val hasAvatar: Boolean
        get() = bytes != null
}
