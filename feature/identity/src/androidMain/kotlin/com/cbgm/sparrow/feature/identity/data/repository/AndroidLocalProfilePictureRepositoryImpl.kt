package com.cbgm.sparrow.feature.identity.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AndroidLocalProfilePictureRepositoryImpl(
    private val context: Context,
    private val preferences: SharedPreferences
) : LocalProfilePictureRepository {
    private val pictureFile = File(context.filesDir, PROFILE_PICTURE_FILE_NAME)
    private val state = MutableStateFlow(loadStoredPicture())

    override fun observe(): Flow<LocalProfilePicture> = state.asStateFlow()

    override suspend fun get(): Result<LocalProfilePicture> =
        runCatching { state.value }

    override suspend fun save(
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(bytes.isNotEmpty()) { "Profile picture must not be empty" }

            val temporaryFile = File(context.filesDir, "$PROFILE_PICTURE_FILE_NAME.tmp")
            temporaryFile.writeBytes(bytes)

            if (pictureFile.exists() && !pictureFile.delete()) {
                temporaryFile.delete()
                error("Existing profile picture could not be replaced")
            }
            if (!temporaryFile.renameTo(pictureFile)) {
                temporaryFile.delete()
                error("Profile picture could not be saved")
            }

            check(
                preferences
                    .edit()
                    .putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds)
                    .commit()
            ) {
                "Profile picture metadata could not be saved"
            }

            state.value =
                LocalProfilePicture(
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    bytes = bytes.copyOf()
                )
        }

    override suspend fun remove(changedAtEpochMilliseconds: Long): Result<Unit> =
        runCatching {
            if (pictureFile.exists()) {
                check(pictureFile.delete()) {
                    "Profile picture could not be removed"
                }
            }

            check(
                preferences
                    .edit()
                    .putLong(PROFILE_PICTURE_CHANGED_AT, changedAtEpochMilliseconds)
                    .commit()
            ) {
                "Profile picture metadata could not be saved"
            }

            state.value =
                LocalProfilePicture(
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    bytes = null
                )
        }

    private fun loadStoredPicture(): LocalProfilePicture =
        LocalProfilePicture(
            changedAtEpochMilliseconds = preferences.getLong(PROFILE_PICTURE_CHANGED_AT, 0L),
            bytes = pictureFile.takeIf(File::isFile)?.readBytes()
        )

    private companion object {
        const val PROFILE_PICTURE_FILE_NAME = "profile-picture.jpg"
        const val PROFILE_PICTURE_CHANGED_AT = "profile_picture_changed_at"
    }
}
