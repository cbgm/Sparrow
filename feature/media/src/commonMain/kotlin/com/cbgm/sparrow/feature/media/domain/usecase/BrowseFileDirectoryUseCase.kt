package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class BrowseFileDirectoryUseCase(
    private val repository: FileBrowserRepository
) {
    suspend operator fun invoke(reference: String): Result<List<FileBrowserEntry>> =
        repository.listDirectory(reference)
}
