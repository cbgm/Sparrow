package com.cbgm.sparrow.core.protocol.profile

import kotlinx.coroutines.flow.Flow

data class RemoteProfilePictureSnapshot(
    val contactId: String,
    val changedAtEpochMilliseconds: Long = 0L,
    val bytes: ByteArray? = null
)

interface RemoteProfilePictureProvider {
    fun observe(contactId: String): Flow<RemoteProfilePictureSnapshot>
}
