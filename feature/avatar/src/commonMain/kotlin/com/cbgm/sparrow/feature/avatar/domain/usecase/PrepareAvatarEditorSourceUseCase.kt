package com.cbgm.sparrow.feature.avatar.domain.usecase

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditorImage
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarEditorRepository

internal class PrepareAvatarEditorSourceUseCase(
    private val repository: AvatarEditorRepository
) {
    suspend operator fun invoke(bytes: ByteArray): Result<AvatarEditorImage> =
        repository.prepareSource(bytes)
}
