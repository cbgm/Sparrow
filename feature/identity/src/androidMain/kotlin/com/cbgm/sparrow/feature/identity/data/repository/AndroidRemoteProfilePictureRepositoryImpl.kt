package com.cbgm.sparrow.feature.identity.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.sparrow.feature.identity.domain.model.RemoteProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.MessageDigest

class AndroidRemoteProfilePictureRepositoryImpl(
    context: Context,
    private val preferences: SharedPreferences
) : RemoteProfilePictureRepository {
    private val pictureDirectory = File(context.filesDir, REMOTE_PROFILE_PICTURE_DIRECTORY).apply { mkdirs() }
    private val state = MutableStateFlow<Map<String, RemoteProfilePicture>>(emptyMap())

    override fun observe(contactId: String): Flow<RemoteProfilePicture> {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        ensureLoaded(contactId)
        return state.map { cached ->
            cached.getValue(contactId)
        }
    }

    override suspend fun get(contactId: String): Result<RemoteProfilePicture> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            state.value[contactId] ?: load(contactId).also { loaded ->
                state.value = state.value + (contactId to loaded)
            }
        }

    override suspend fun save(
        contactId: String,
        bytes: ByteArray,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            require(bytes.isNotEmpty()) { "Remote profile picture must not be empty" }

            val target = pictureFile(contactId)
            val temporary = File(target.parentFile, "${target.name}.tmp")
            temporary.writeBytes(bytes)
            if (target.exists() && !target.delete()) {
                temporary.delete()
                error("Existing remote profile picture could not be replaced")
            }
            if (!temporary.renameTo(target)) {
                temporary.delete()
                error("Remote profile picture could not be saved")
            }

            check(
                preferences
                    .edit()
                    .putLong(changedAtKey(contactId), changedAtEpochMilliseconds)
                    .commit()
            ) {
                "Remote profile-picture metadata could not be saved"
            }

            state.value =
                state.value +
                (
                    contactId to
                        RemoteProfilePicture(
                            contactId = contactId,
                            changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                            bytes = bytes.copyOf()
                        )
                )
        }

    override suspend fun remove(
        contactId: String,
        changedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
            val target = pictureFile(contactId)
            if (target.exists()) {
                check(target.delete()) { "Remote profile picture could not be removed" }
            }

            check(
                preferences
                    .edit()
                    .putLong(changedAtKey(contactId), changedAtEpochMilliseconds)
                    .commit()
            ) {
                "Remote profile-picture metadata could not be saved"
            }

            state.value =
                state.value +
                (
                    contactId to
                        RemoteProfilePicture(
                            contactId = contactId,
                            changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                            bytes = null
                        )
                )
        }

    private fun ensureLoaded(contactId: String) {
        if (state.value[contactId] == null) {
            state.value = state.value + (contactId to load(contactId))
        }
    }

    private fun load(contactId: String): RemoteProfilePicture =
        RemoteProfilePicture(
            contactId = contactId,
            changedAtEpochMilliseconds = preferences.getLong(changedAtKey(contactId), 0L),
            bytes = pictureFile(contactId).takeIf(File::isFile)?.readBytes()
        )

    private fun pictureFile(contactId: String): File =
        File(pictureDirectory, "${contactId.sha256Hex()}.jpg")

    private fun changedAtKey(contactId: String): String =
        "$REMOTE_PROFILE_PICTURE_CHANGED_AT_PREFIX$contactId"

    private fun String.sha256Hex(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val REMOTE_PROFILE_PICTURE_DIRECTORY = "remote-profile-pictures"
        const val REMOTE_PROFILE_PICTURE_CHANGED_AT_PREFIX = "remote_profile_picture_changed_at_"
    }
}
