package com.cbgm.sparrow.server.linkpreview

import kotlinx.serialization.Serializable

@Serializable
data class LinkPreviewRequest(
    val url: String
)

@Serializable
data class LinkPreviewResponse(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val siteName: String? = null,
    val imagePath: String? = null
)

data class FetchedLinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val siteName: String?,
    val imageUrl: String?
)

data class LinkPreviewImage(
    val bytes: ByteArray,
    val contentType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinkPreviewImage

        if (!bytes.contentEquals(other.bytes)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
