package com.cbgm.sparrow.core.asset.domain.model

sealed interface Asset {
    val id: String
}

data class Text(
    override val id: String,
    val text: String
) : Asset

data class Image(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val fileName: String? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : Asset

data class Video(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val thumbnailFilePath: String? = null
) : Asset

data class File(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String,
    val localFilePath: String? = null
) : Asset

data class Voice(
    override val id: String,
    val mimeType: String,
    val byteSize: Long,
    val durationMilliseconds: Long
) : Asset

data class Location(
    override val id: String
) : Asset

data class Contact(
    override val id: String
) : Asset

data class Poll(
    override val id: String,
    val question: String,
    val description: String? = null,
    val options: List<PollOption> = emptyList(),
    val images: List<Image> = emptyList(),
    val allowMultipleSelection: Boolean = false,
    val allowVoteChange: Boolean = true,
    val isAnonymous: Boolean = false,
    val expiresAtEpochMilliseconds: Long? = null,
    val closedAtEpochMilliseconds: Long? = null
) : Asset

data class PollOption(
    val id: String,
    val text: String
)
