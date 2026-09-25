package com.cbgm.sparrow.feature.linkpreview.domain.usecase

import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository

class GetLinkPreviewUseCase(
    private val repository: LinkPreviewRepository
) {
    suspend operator fun invoke(url: String) = repository.getPreview(url)
}
