package com.cbgm.sparrow.feature.identity.data.profile

import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureMetadataProcessor
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Applies authenticated sender profile-picture metadata after packet validation. */
class IdentityRemoteProfilePictureMetadataProcessor(
    private val repository: RemoteProfilePictureRepository
) : RemoteProfilePictureMetadataProcessor {
    private val mutex = Mutex()

    override suspend fun apply(
        contactId: String,
        metadata: ProfilePictureMetadata
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            mutex.withLock {
                val current = repository.get(contactId).getOrThrow()
                if (metadata.changedAtEpochMilliseconds <= current.changedAtEpochMilliseconds) {
                    return@withLock
                }

                when {
                    !metadata.hasPicture ->
                        repository
                            .remove(
                                contactId = contactId,
                                changedAtEpochMilliseconds = metadata.changedAtEpochMilliseconds
                            ).getOrThrow()

                    metadata.payload != null ->
                        repository
                            .save(
                                contactId = contactId,
                                bytes = metadata.payload!!.bytes,
                                changedAtEpochMilliseconds = metadata.changedAtEpochMilliseconds
                            ).getOrThrow()

                    else -> {
                        // A newer picture exists, but this packet intentionally omitted the bytes.
                        // Keep the previous timestamp so a later packet carrying the same version
                        // can still install the image.
                    }
                }
            }
        }
}
