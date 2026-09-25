package com.cbgm.sparrow.feature.avatar.data.repository

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.avatar.data.datasource.LocalAvatarEditorDataSource
import com.cbgm.sparrow.feature.avatar.device.cropAndEncodeProfilePicture
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditorImage
import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarEditorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

internal class AvatarEditorRepositoryImpl(
    private val localDataSource: LocalAvatarEditorDataSource
) : AvatarEditorRepository {
    private val logger = SparrowLog.withTag("AvatarEditorRepository")

    override suspend fun prepareSource(bytes: ByteArray): Result<AvatarEditorImage> =
        runLogged("Failed to prepare avatar source") {
            require(bytes.isNotEmpty()) { "Avatar source must not be empty" }
            localDataSource.setSource(bytes)
            AvatarEditorImage(
                image =
                    withContext(Dispatchers.Default) {
                        bytes.decodeToImageBitmap()
                    }
            )
        }

    override suspend fun crop(cropRegion: ProfilePictureCropRegion): Result<AvatarEditResult> =
        runLogged("Failed to crop avatar") {
            val sourceBytes =
                requireNotNull(localDataSource.getSource()) {
                    "Avatar editor source is not available"
                }
            val cropped =
                cropAndEncodeProfilePicture(
                    sourceBytes = sourceBytes,
                    cropRegion = cropRegion
                )
            localDataSource.saveResult(cropped).also {
                localDataSource.clearSource()
            }
        }

    override suspend fun consumeResult(result: AvatarEditResult): Result<ByteArray> =
        runLogged("Failed to consume avatar edit result") {
            requireNotNull(localDataSource.consumeResult(result)) {
                "Avatar edit result is no longer available"
            }
        }

    override fun clear() {
        localDataSource.clearSource()
    }

    private suspend fun <T> runLogged(
        message: String,
        block: suspend () -> T
    ): Result<T> =
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            logger.error(error) { message }
            Result.failure(error)
        }
}
