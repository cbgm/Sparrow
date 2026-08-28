package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.LocalProfilePicture
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import kotlinx.coroutines.flow.Flow

class ObserveLocalProfilePictureUseCase(
    private val repository: LocalProfilePictureRepository
) {
    operator fun invoke(): Flow<LocalProfilePicture> = repository.observe()
}
