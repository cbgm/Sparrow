package com.cbgm.sparrow.protocol.profile

interface LocalProfilePictureMetadataSource {
    suspend fun forInvite(): Result<ProfilePictureMetadata>

    suspend fun forMessage(): Result<ProfilePictureMetadata>
}
