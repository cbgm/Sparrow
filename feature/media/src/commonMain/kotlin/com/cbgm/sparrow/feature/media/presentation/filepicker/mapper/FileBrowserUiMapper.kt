package com.cbgm.sparrow.feature.media.presentation.filepicker.mapper

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FileBrowserEntryKindUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FileBrowserEntryUi
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

internal fun FileBrowserEntry.toFileBrowserEntryUi(blockedSourceReferences: Set<String>): FileBrowserEntryUi =
    FileBrowserEntryUi(
        reference = reference,
        displayName = displayName,
        kind = toFileBrowserEntryKindUi(),
        sizeText = byteSize?.toReadableByteSize(),
        typeText = toTypeText(),
        isBlocked = !isDirectory && sourceReference != null && sourceReference in blockedSourceReferences
    )

private fun FileBrowserEntry.toFileBrowserEntryKindUi(): FileBrowserEntryKindUi {
    if (isDirectory) return FileBrowserEntryKindUi.DIRECTORY
    val mime = mimeType.orEmpty().lowercase()
    val extension = displayName.substringAfterLast('.', "").lowercase()
    return when {
        mime.startsWith("image/") -> FileBrowserEntryKindUi.IMAGE
        mime.startsWith("video/") -> FileBrowserEntryKindUi.VIDEO
        mime.startsWith("audio/") -> FileBrowserEntryKindUi.AUDIO
        mime == "application/pdf" || extension == "pdf" -> FileBrowserEntryKindUi.PDF
        mime.startsWith("text/") || extension in TEXT_EXTENSIONS -> FileBrowserEntryKindUi.TEXT
        extension in ARCHIVE_EXTENSIONS -> FileBrowserEntryKindUi.ARCHIVE
        else -> FileBrowserEntryKindUi.OTHER
    }
}

private fun FileBrowserEntry.toTypeText(): String? {
    if (isDirectory) return null
    val extension = displayName.substringAfterLast('.', "").takeIf(String::isNotBlank)
    if (extension != null) return extension.uppercase()
    return mimeType
        ?.substringAfter('/', missingDelimiterValue = mimeType)
        ?.replace('-', ' ')
        ?.uppercase()
        ?.takeIf(String::isNotBlank)
}

private val TEXT_EXTENSIONS = setOf("txt", "md", "json", "xml", "csv", "yaml", "yml", "log")
private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2")
