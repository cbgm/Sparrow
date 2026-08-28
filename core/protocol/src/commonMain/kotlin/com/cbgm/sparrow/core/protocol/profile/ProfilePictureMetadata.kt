package com.cbgm.sparrow.core.protocol.profile

import com.cbgm.sparrow.core.protocol.serializer.ByteArrayAsBase64Serializer
import kotlinx.serialization.Serializable

/**
 * Sender profile-picture state piggybacked on existing Sparrow packets.
 *
 * [payload] is deliberately optional: normal messages may announce the
 * current timestamp/state without retransmitting image bytes every time.
 */
@Serializable
data class ProfilePictureMetadata(
    val changedAtEpochMilliseconds: Long = 0L,
    val hasPicture: Boolean = false,
    val payload: ProfilePicturePayload? = null
) {
    init {
        require(changedAtEpochMilliseconds >= 0L) {
            "Profile-picture timestamp must not be negative"
        }
        require(hasPicture || payload == null) {
            "A removed profile picture cannot contain image bytes"
        }
    }
}

@Serializable
class ProfilePicturePayload(
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val bytes: ByteArray
) {
    init {
        require(bytes.isNotEmpty()) {
            "Profile-picture payload must not be empty"
        }
        require(bytes.size <= MAX_PROFILE_PICTURE_BYTES) {
            "Profile-picture payload must not exceed $MAX_PROFILE_PICTURE_BYTES bytes"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ProfilePicturePayload && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    private companion object {
        const val MAX_PROFILE_PICTURE_BYTES = 1_048_576
    }
}
