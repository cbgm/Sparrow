package com.cbgm.sparrow.feature.avatar.domain.usecase

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult
import com.cbgm.sparrow.feature.avatar.domain.model.ProfilePictureCropRegion
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarEditorRepository

internal class CropAvatarEditorSourceUseCase(
    private val repository: AvatarEditorRepository
) {
    suspend operator fun invoke(cropRegion: ProfilePictureCropRegion): Result<AvatarEditResult> =
        repository.crop(cropRegion)
}
