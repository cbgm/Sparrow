package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

/** The group avatar image (null when removed) and when an admin last changed it; 0 means never published. */
class GroupAvatarState(
    val groupId: String,
    val bytes: ByteArray?,
    val changedAtEpochMilliseconds: Long
) {
    val isPublished: Boolean get() = changedAtEpochMilliseconds > 0L

    override fun equals(other: Any?): Boolean =
        other is GroupAvatarState &&
            groupId == other.groupId &&
            changedAtEpochMilliseconds == other.changedAtEpochMilliseconds &&
            bytes.contentEquals(other.bytes)

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + changedAtEpochMilliseconds.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}
