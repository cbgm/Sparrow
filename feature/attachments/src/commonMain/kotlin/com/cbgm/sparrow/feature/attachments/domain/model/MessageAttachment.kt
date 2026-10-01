package com.cbgm.sparrow.feature.attachments.domain.model

sealed interface MessageAttachment {
    val id: String

    data class Image(
        override val id: String,
        val mimeType: String,
        val byteSize: Long,
        val width: Int,
        val height: Int,
        val localFilePath: String? = null
    ) : MessageAttachment

    data class Video(
        override val id: String,
        val mimeType: String,
        val byteSize: Long,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null
    ) : MessageAttachment

    data class File(
        override val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null
    ) : MessageAttachment

    data class Voice(
        override val id: String,
        val mimeType: String,
        val byteSize: Long,
        val durationMilliseconds: Long
    ) : MessageAttachment

    data class Location(
        override val id: String
    ) : MessageAttachment

    data class Contact(
        override val id: String
    ) : MessageAttachment
}
