package com.cbgm.sparrow.feature.linkpreview.domain.usecase

import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository
import com.cbgm.sparrow.feature.transport.domain.usecase.GetCurrentNodeBaseUrlUseCase

class PrefetchLinkPreviewsUseCase(
    private val repository: LinkPreviewRepository,
    private val getCurrentNodeBaseUrl: GetCurrentNodeBaseUrlUseCase
) {
    operator fun invoke(text: String): Result<Unit> = repository.fetchPreviews(text, getCurrentNodeBaseUrl())
}
