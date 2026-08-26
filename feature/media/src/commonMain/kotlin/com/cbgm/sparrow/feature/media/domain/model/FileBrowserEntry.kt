package com.cbgm.sparrow.feature.media.domain.model

data class FileBrowserEntry(
    val reference: String,
    val sourceReference: String?,
    val displayName: String,
    val isDirectory: Boolean,
    val byteSize: Long?,
    val mimeType: String?
)
