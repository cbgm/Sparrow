package com.cbgm.sparrow.feature.media.data.mapper

import com.cbgm.sparrow.feature.media.data.model.FileBrowserContentDto
import com.cbgm.sparrow.feature.media.data.model.FileBrowserDirectoryDto
import com.cbgm.sparrow.feature.media.data.model.FileBrowserEntryDto
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserContent
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry

fun FileBrowserContentDto.toFileBrowserContent(): FileBrowserContent =
    FileBrowserContent(
        sourceReference = sourceReference,
        displayName = displayName,
        mimeType = mimeType,
        bytes = bytes
    )

fun FileBrowserDirectoryDto.toFileBrowserDirectory(): FileBrowserDirectory =
    FileBrowserDirectory(
        reference = reference,
        displayName = displayName
    )

fun FileBrowserEntryDto.toFileBrowserEntry(): FileBrowserEntry =
    FileBrowserEntry(
        reference = reference,
        sourceReference = sourceReference,
        displayName = displayName,
        isDirectory = isDirectory,
        byteSize = byteSize,
        mimeType = mimeType
    )
