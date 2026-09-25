package com.cbgm.sparrow.feature.linkpreview.data.mapper

import com.cbgm.sparrow.data.database.entity.LinkPreviewEntity
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDto
import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview

fun LinkPreviewDto.toDomain(): LinkPreview =
    LinkPreview(
        url = url,
        title = title,
        description = description,
        siteName = siteName,
        imageBytes = imageBytes
    )

fun LinkPreviewDto.toEntity(): LinkPreviewEntity =
    LinkPreviewEntity(
        url = url,
        title = title,
        description = description,
        siteName = siteName,
        imageBytes = imageBytes
    )

fun LinkPreviewEntity.toDomain(): LinkPreview =
    LinkPreview(
        url = url,
        title = title,
        description = description,
        siteName = siteName,
        imageBytes = imageBytes
    )
