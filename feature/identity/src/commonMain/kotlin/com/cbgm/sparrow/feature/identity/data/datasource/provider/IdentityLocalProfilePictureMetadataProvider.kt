package com.cbgm.sparrow.feature.identity.data.datasource.provider

import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.ProfilePicturePayload
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository

/** Creates profile-picture metadata for packets without introducing profile-only packets. */
class IdentityLocalProfilePictureMetadataProvider(
    private val repository: LocalProfilePictureRepository
) : LocalProfilePictureMetadataProvider {
    override suspend fun forInvite(): Result<ProfilePictureMetadata> =
        repository.get().map { picture ->
            picture.toMetadata(includeBytes = true)
        }

    override suspend fun forMessage(): Result<ProfilePictureMetadata> =
        repository.get().map { picture ->
            val age =
                (SystemClock.nowEpochMilliseconds() - picture.changedAtEpochMilliseconds)
                    .coerceAtLeast(0L)
            picture.toMetadata(
                includeBytes =
                    picture.hasPicture &&
                        picture.changedAtEpochMilliseconds > 0L &&
                        age <= RECENT_CHANGE_WINDOW_MILLISECONDS
            )
        }

    private fun LocalProfilePicture.toMetadata(includeBytes: Boolean): ProfilePictureMetadata =
        ProfilePictureMetadata(
            changedAtEpochMilliseconds = changedAtEpochMilliseconds,
            hasPicture = hasPicture,
            payload =
                bytes
                    ?.takeIf { includeBytes }
                    ?.let { imageBytes -> ProfilePicturePayload(imageBytes.copyOf()) }
        )

    private companion object {
        const val RECENT_CHANGE_WINDOW_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
