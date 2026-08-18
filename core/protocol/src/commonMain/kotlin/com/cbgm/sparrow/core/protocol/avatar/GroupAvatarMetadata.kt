package com.cbgm.sparrow.core.protocol.avatar

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
data class GroupAvatarMetadata(
    val changedAtEpochMilliseconds: Long = 0L,
    val hasAvatar: Boolean = false,
    val payload: GroupAvatarPayload? = null
) {
    init {
        require(changedAtEpochMilliseconds >= 0L) { "Group-avatar timestamp must not be negative" }
        require(hasAvatar == (payload != null)) {
            "Group-avatar presence must match its image payload"
        }
    }
}

@Serializable
class GroupAvatarPayload(
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val bytes: ByteArray
) {
    init {
        require(bytes.isNotEmpty()) { "Group-avatar payload must not be empty" }
        require(bytes.size <= MAX_GROUP_AVATAR_BYTES) {
            "Group-avatar payload must not exceed $MAX_GROUP_AVATAR_BYTES bytes"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is GroupAvatarPayload && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    private companion object {
        const val MAX_GROUP_AVATAR_BYTES = 1_048_576
    }
}
