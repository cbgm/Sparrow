package com.cbgm.sparrow.feature.avatar.domain.usecase

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarImage
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow

class ObserveAvatarUseCase(
    private val repository: AvatarRepository
) {
    operator fun invoke(target: AvatarTarget): Flow<Result<AvatarImage>> = repository.observe(target)
}
