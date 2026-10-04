package com.cbgm.sparrow.protocol.profile

import kotlinx.coroutines.flow.Flow

data class RemoteProfilePictureSnapshot(
    val contactId: String,
    val changedAtEpochMilliseconds: Long = 0L,
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteProfilePictureSnapshot) return false

        return contactId == other.contactId &&
            changedAtEpochMilliseconds == other.changedAtEpochMilliseconds &&
            when {
                bytes == null -> other.bytes == null
                other.bytes == null -> false
                else -> bytes.contentEquals(other.bytes)
            }
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()
        result = 31 * result + changedAtEpochMilliseconds.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

interface RemoteProfilePictureProvider {
    fun observe(contactId: String): Flow<RemoteProfilePictureSnapshot>
}
