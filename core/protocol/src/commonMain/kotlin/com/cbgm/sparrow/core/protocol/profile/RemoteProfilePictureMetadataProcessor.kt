package com.cbgm.sparrow.core.protocol.profile

interface RemoteProfilePictureMetadataProcessor {
    suspend fun apply(
        contactId: String,
        metadata: ProfilePictureMetadata
    ): Result<Unit>
}
