package com.cbgm.sparrow.feature.linkpreview.domain.usecase

import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository

class PrefetchLinkPreviewsUseCase(
    private val repository: LinkPreviewRepository
) {
    operator fun invoke(text: String) {
        repository.fetchPreviews(text)
    }
}
