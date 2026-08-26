package com.cbgm.sparrow.feature.media.data.mapper

import com.cbgm.sparrow.feature.media.data.model.FileBrowserContentData
import com.cbgm.sparrow.feature.media.data.model.FileBrowserDirectoryData
import com.cbgm.sparrow.feature.media.data.model.FileBrowserEntryData
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry

fun FileBrowserContentData.toDomain(): FileBrowserContent =
    FileBrowserContent(
        sourceReference = sourceReference,
        displayName = displayName,
        mimeType = mimeType,
        bytes = bytes
    )

fun FileBrowserDirectoryData.toDomain(): FileBrowserDirectory =
    FileBrowserDirectory(
        reference = reference,
        displayName = displayName
    )

fun FileBrowserEntryData.toDomain(): FileBrowserEntry =
    FileBrowserEntry(
        reference = reference,
        sourceReference = sourceReference,
        displayName = displayName,
        isDirectory = isDirectory,
        byteSize = byteSize,
        mimeType = mimeType
    )
