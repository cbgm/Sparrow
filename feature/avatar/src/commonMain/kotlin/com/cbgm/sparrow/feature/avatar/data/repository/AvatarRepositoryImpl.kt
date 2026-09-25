package com.cbgm.sparrow.feature.avatar.data.repository

import com.cbgm.sparrow.core.coroutines.ApplicationCoroutineScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.feature.avatar.data.datasource.AvatarDataSource
import com.cbgm.sparrow.feature.avatar.data.datasource.LocalAvatarImageDataSource
import com.cbgm.sparrow.feature.avatar.domain.model.Avatar
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarImage
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

internal class AvatarRepositoryImpl(
    private val dataSource: AvatarDataSource,
    private val localAvatarImageDataSource: LocalAvatarImageDataSource,
    private val applicationScope: ApplicationCoroutineScope
) : AvatarRepository {
    private val logger = SparrowLog.withTag("AvatarRepository")
    private val cachedFlows = mutableMapOf<AvatarTarget, Flow<Result<AvatarImage>>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(target: AvatarTarget): Flow<Result<AvatarImage>> =
        cachedFlows.getOrPut(target) {
            dataSource
                .observe(target)
                .distinctUntilChangedBy(Avatar::changedAtEpochMilliseconds)
                .mapLatest { avatar -> Result.success(resolveImage(avatar)) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    logger.error(error) { "Failed to load ${target.description()} avatar" }
                    emit(Result.failure(error))
                }.shareIn(
                    scope = applicationScope,
                    started = SharingStarted.Eagerly,
                    replay = 1
                )
        }

    private suspend fun resolveImage(avatar: Avatar): AvatarImage {
        val cached = localAvatarImageDataSource.get(avatar.target)
        if (cached?.changedAtEpochMilliseconds == avatar.changedAtEpochMilliseconds) {
            return AvatarImage(
                image = cached.image,
                changedAtEpochMilliseconds = cached.changedAtEpochMilliseconds
            )
        }

        val image =
            avatar.bytes
                ?.takeIf(ByteArray::isNotEmpty)
                ?.let { bytes ->
                    withContext(Dispatchers.Default) {
                        bytes.decodeToImageBitmap()
                    }
                }

        localAvatarImageDataSource.save(
            target = avatar.target,
            changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds,
            image = image
        )

        return AvatarImage(
            image = image,
            changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds
        )
    }

    private fun AvatarTarget.description(): String =
        when (this) {
            AvatarTarget.LocalUser -> "local user"
            is AvatarTarget.User -> "user '$id'"
            is AvatarTarget.Group -> "group '$id'"
        }
}
