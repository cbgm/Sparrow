package com.cbgm.sparrow.feature.linkpreview.domain.repository

import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview

interface LinkPreviewRepository {
    /** [nodeBaseUrl]: the connected node, which fetches the previews; null while none is connected. */
    fun fetchPreviews(
        text: String,
        nodeBaseUrl: String?
    ): Result<Unit>

    suspend fun getPreview(
        url: String,
        nodeBaseUrl: String?
    ): Result<LinkPreview>
}
