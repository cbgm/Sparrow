package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor

class ApplyRemoteProfilePictureMetadataUseCase(
    private val processor: RemoteProfilePictureMetadataProcessor
) {
    suspend operator fun invoke(
        contactId: String,
        metadata: ProfilePictureMetadata
    ): Result<Unit> =
        processor.apply(contactId, metadata)
}
