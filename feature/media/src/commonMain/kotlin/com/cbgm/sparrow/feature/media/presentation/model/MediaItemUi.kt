package com.cbgm.sparrow.feature.media.presentation.model

enum class MediaTypeUi { IMAGE, VIDEO }

interface VisualMediaUi {
    val id: String
    val type: MediaTypeUi
    val mimeType: String
    val localFilePath: String?
    val thumbnailFilePath: String?
    val width: Int?
    val height: Int?
    val durationMilliseconds: Long?
}

data class MediaItemUi(
    override val id: String,
    override val type: MediaTypeUi,
    override val mimeType: String,
    override val localFilePath: String? = null,
    override val thumbnailFilePath: String? = null,
    override val width: Int? = null,
    override val height: Int? = null,
    override val durationMilliseconds: Long? = null
) : VisualMediaUi
