package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class SetFileBrowserRootUseCase(
    private val repository: FileBrowserRepository
) {
    suspend operator fun invoke(reference: String): Result<Unit> =
        repository.setRootDirectory(reference)
}
