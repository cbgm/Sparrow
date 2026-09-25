package com.cbgm.sparrow.feature.linkpreview.presentation.mapper

import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview
import com.cbgm.sparrow.feature.linkpreview.presentation.model.LinkPreviewUi

fun LinkPreview.toUi(): LinkPreviewUi =
    LinkPreviewUi(
        url = url,
        title = title,
        description = description,
        siteName = siteName,
        imageBytes = imageBytes
    )
