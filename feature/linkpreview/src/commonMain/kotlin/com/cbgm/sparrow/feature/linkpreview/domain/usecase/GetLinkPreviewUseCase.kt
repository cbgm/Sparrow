package com.cbgm.sparrow.feature.linkpreview.domain.usecase

import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository
import com.cbgm.sparrow.feature.transport.domain.usecase.GetCurrentNodeBaseUrlUseCase

class GetLinkPreviewUseCase(
    private val repository: LinkPreviewRepository,
    private val getCurrentNodeBaseUrl: GetCurrentNodeBaseUrlUseCase
) {
    suspend operator fun invoke(url: String) = repository.getPreview(url, getCurrentNodeBaseUrl())
}
