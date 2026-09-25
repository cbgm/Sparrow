package com.cbgm.sparrow.feature.avatar.domain.repository

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditorImage
import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion

internal interface AvatarEditorRepository {
    suspend fun prepareSource(bytes: ByteArray): Result<AvatarEditorImage>

    suspend fun crop(cropRegion: ProfilePictureCropRegion): Result<AvatarEditResult>

    suspend fun consumeResult(result: AvatarEditResult): Result<ByteArray>

    fun clear()
}
