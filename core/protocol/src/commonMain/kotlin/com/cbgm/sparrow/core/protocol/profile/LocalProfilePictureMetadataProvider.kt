package com.cbgm.sparrow.core.protocol.profile

interface LocalProfilePictureMetadataProvider {
    suspend fun forInvite(): Result<ProfilePictureMetadata>

    suspend fun forMessage(): Result<ProfilePictureMetadata>
}
