package com.cbgm.sparrow.feature.media.data.model

data class FileBrowserEntryData(
    val reference: String,
    val sourceReference: String?,
    val displayName: String,
    val isDirectory: Boolean,
    val byteSize: Long?,
    val mimeType: String?
)
