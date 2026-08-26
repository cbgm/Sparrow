package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class GetFileBrowserRootUseCase(
    private val repository: FileBrowserRepository
) {
    suspend operator fun invoke(): Result<FileBrowserDirectory> = repository.getRootDirectory()
}
