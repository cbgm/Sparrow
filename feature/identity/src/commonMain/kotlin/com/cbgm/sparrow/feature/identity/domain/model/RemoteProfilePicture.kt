package com.cbgm.sparrow.feature.identity.domain.model

data class RemoteProfilePicture(
    val contactId: String,
    val changedAtEpochMilliseconds: Long = 0L,
    val bytes: ByteArray? = null
) {
    val hasPicture: Boolean
        get() = bytes != null
}
