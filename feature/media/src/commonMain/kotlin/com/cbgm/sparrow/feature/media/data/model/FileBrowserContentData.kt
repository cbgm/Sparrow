package com.cbgm.sparrow.feature.media.data.model

data class FileBrowserContentData(
    val sourceReference: String,
    val displayName: String,
    val mimeType: String,
    val bytes: ByteArray
)
