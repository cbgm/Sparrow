package com.cbgm.sparrow.protocol.profile

interface RemoteProfilePictureMetadataProcessor {
    suspend fun apply(
        contactId: String,
        metadata: ProfilePictureMetadata
    ): Result<Unit>
}
