package com.cbgm.sparrow.feature.linkpreview.domain.repository

import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview

interface LinkPreviewRepository {
    fun fetchPreviews(text: String): Result<Unit>

    suspend fun getPreview(url: String): Result<LinkPreview>
}
