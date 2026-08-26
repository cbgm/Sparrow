package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.presentation.model.FileSelection

fun FileBrowserContent.toFileSelection(): FileSelection =
    FileSelection(
        id = IdGenerator.generate(prefix = "file"),
        bytes = bytes,
        mimeType = mimeType,
        fileName = displayName,
        sourceReference = sourceReference
    )
