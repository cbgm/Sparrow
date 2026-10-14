package com.cbgm.sparrow.feature.avatar.domain.usecase

import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarEditorRepository

internal class ClearAvatarEditorUseCase(
    private val repository: AvatarEditorRepository
) {
    operator fun invoke(): Result<Unit> = repository.clear()
}
