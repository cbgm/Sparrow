package com.cbgm.sparrow.feature.media.data.mapper

import com.cbgm.sparrow.feature.media.data.model.StoredMediaSelectionPathsDto
import com.cbgm.sparrow.feature.media.domain.model.StoredMediaSelectionPaths

fun StoredMediaSelectionPathsDto.toStoredMediaSelectionPaths(): StoredMediaSelectionPaths =
    StoredMediaSelectionPaths(localFilePath = localFilePath, thumbnailFilePath = thumbnailFilePath)
