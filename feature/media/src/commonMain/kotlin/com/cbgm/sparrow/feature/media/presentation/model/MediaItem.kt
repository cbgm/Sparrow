package com.cbgm.sparrow.feature.media.presentation.model

enum class MediaType { IMAGE, VIDEO }

/** UI models refer to media on disk; binary payloads never enter Compose state. */
data class MediaItem(
    val id: String,
    val type: MediaType,
    val mimeType: String,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null
)
