package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureMetadataProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureSnapshot
import com.cbgm.sparrow.core.protocol.profile.ProfilePictureMetadata
import com.cbgm.sparrow.core.protocol.profile.ProfilePicturePayload
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalProfilePictureDataSource(
    private val dataStore: SparrowDataStore,
    private val fileDataSource: ProfilePictureFileDataSource
) : LocalProfilePictureProvider,
    LocalProfilePictureMetadataProvider {
    fun observePicture(): Flow<LocalProfilePicture> =
        dataStore.observeLong(PROFILE_PICTURE_CHANGED_AT).map { changedAt ->
            LocalProfilePicture(
                changedAtEpochMilliseconds = changedAt,
                bytes = fileDataSource.readLocal()
            )
        }

    suspend fun get(): LocalProfilePicture = LocalProfilePicture(
        changedAtEpochMilliseconds = dataStore.getLong(PROFILE_PICTURE_CHANGED_AT),
        bytes = fileDataSource.readLocal()
    )

    suspend fun save(
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ) {
        fileDataSource.writeLocal(bytes)
        dataStore.edit { putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds) }
    }

    suspend fun remove(changedAtEpochMilliseconds: Long) {
        fileDataSource.deleteLocal()
        dataStore.edit { putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds) }
    }

    override fun observe(): Flow<LocalProfilePictureSnapshot> =
        observePicture().map { picture ->
            LocalProfilePictureSnapshot(
                changedAtEpochMilliseconds = picture.changedAtEpochMilliseconds,
                bytes = picture.bytes
            )
        }

    override suspend fun forInvite(): Result<ProfilePictureMetadata> =
        runCatching { get().toMetadata(includeBytes = true) }

    override suspend fun forMessage(): Result<ProfilePictureMetadata> =
        runCatching {
            val picture = get()
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
            payload = bytes?.takeIf { includeBytes }?.let { ProfilePicturePayload(it.copyOf()) }
        )

    private companion object {
        const val PROFILE_PICTURE_CHANGED_AT = "identity.profile_picture.changed_at"
        const val RECENT_CHANGE_WINDOW_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}
