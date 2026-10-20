package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.usecase

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.identity.domain.usecase.ApplyRemoteProfilePictureMetadataUseCase
import com.cbgm.sparrow.protocol.profile.ProfilePictureMetadata

/** Profile pictures are best effort: a failure is logged and never fails the surrounding workflow. */
class ApplyPeerProfilePictureUseCase(
    private val applyRemoteProfilePictureMetadata: ApplyRemoteProfilePictureMetadataUseCase
) {
    private val logger = SparrowLog.withTag("ApplyPeerProfilePictureUseCase")

    suspend operator fun invoke(contactId: String, metadata: ProfilePictureMetadata): Result<Unit> =
        applyRemoteProfilePictureMetadata(contactId, metadata)
            .onFailure { error -> logger.warn(error) { "Could not store profile picture for $contactId" } }
}
