package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class ReadFileBrowserEntryUseCase(
    private val repository: FileBrowserRepository
) {
    suspend operator fun invoke(reference: String, maxByteSize: Long): Result<FileBrowserContent> =
        repository.readFile(reference, maxByteSize)
}
