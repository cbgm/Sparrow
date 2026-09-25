package com.cbgm.sparrow.feature.linkpreview.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LinkPreviewRequestDto(
    val url: String
)

@Serializable
data class LinkPreviewDto(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val siteName: String? = null,
    val imagePath: String? = null,
    @Transient
    val imageBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinkPreviewDto

        if (url != other.url) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (siteName != other.siteName) return false
        if (imagePath != other.imagePath) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (siteName?.hashCode() ?: 0)
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}
