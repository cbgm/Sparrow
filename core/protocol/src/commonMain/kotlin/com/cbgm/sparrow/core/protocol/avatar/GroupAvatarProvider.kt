package com.cbgm.sparrow.core.protocol.avatar

import kotlinx.coroutines.flow.Flow

data class GroupAvatarSnapshot(
    val groupId: String,
    val changedAtEpochMilliseconds: Long = 0L,
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GroupAvatarSnapshot

        if (changedAtEpochMilliseconds != other.changedAtEpochMilliseconds) return false
        if (groupId != other.groupId) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = changedAtEpochMilliseconds.hashCode()
        result = 31 * result + groupId.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

interface GroupAvatarProvider {
    fun observeSnapshot(groupId: String): Flow<GroupAvatarSnapshot>
}
