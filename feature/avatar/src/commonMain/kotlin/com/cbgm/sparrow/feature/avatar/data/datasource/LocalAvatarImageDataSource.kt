package com.cbgm.sparrow.feature.avatar.data.datasource

import androidx.compose.ui.graphics.ImageBitmap
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class CachedAvatarImage(
    val changedAtEpochMilliseconds: Long,
    val image: ImageBitmap?
)

internal class LocalAvatarImageDataSource {
    private val mutex = Mutex()
    private val entries = mutableMapOf<AvatarTarget, CachedAvatarImage>()

    suspend fun get(target: AvatarTarget): CachedAvatarImage? =
        mutex.withLock { entries[target] }

    suspend fun save(
        target: AvatarTarget,
        changedAtEpochMilliseconds: Long,
        image: ImageBitmap?
    ) {
        mutex.withLock {
            entries[target] =
                CachedAvatarImage(
                    changedAtEpochMilliseconds = changedAtEpochMilliseconds,
                    image = image
                )
        }
    }
}
