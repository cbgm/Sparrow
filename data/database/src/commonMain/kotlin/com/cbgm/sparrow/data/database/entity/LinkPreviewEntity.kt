package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "link_previews")
data class LinkPreviewEntity(
    @PrimaryKey
    val url: String,
    val title: String?,
    val description: String?,
    val siteName: String?,
    val imageBytes: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinkPreviewEntity

        if (url != other.url) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (siteName != other.siteName) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (siteName?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}
