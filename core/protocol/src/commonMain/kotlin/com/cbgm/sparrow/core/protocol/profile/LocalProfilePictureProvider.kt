package com.cbgm.sparrow.core.protocol.profile

import kotlinx.coroutines.flow.Flow

data class LocalProfilePictureSnapshot(
    val changedAtEpochMilliseconds: Long = 0L,
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalProfilePictureSnapshot

        if (changedAtEpochMilliseconds != other.changedAtEpochMilliseconds) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = changedAtEpochMilliseconds.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

interface LocalProfilePictureProvider {
    fun observe(): Flow<LocalProfilePictureSnapshot>
}
