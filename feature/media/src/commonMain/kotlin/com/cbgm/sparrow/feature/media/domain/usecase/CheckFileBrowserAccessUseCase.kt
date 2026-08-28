package com.cbgm.sparrow.feature.media.domain.usecase

import com.cbgm.sparrow.feature.media.domain.repository.FileBrowserRepository

class CheckFileBrowserAccessUseCase(
    private val repository: FileBrowserRepository
) {
    operator fun invoke(): Boolean = repository.hasFileAccess()
}
