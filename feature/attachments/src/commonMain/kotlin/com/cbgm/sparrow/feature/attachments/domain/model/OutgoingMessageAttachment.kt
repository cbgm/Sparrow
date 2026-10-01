package com.cbgm.sparrow.feature.attachments.domain.model

sealed interface OutgoingMessageAttachment {
    val id: String
    val bytes: ByteArray

    data class Image(
        override val id: String,
        override val bytes: ByteArray,
        val mimeType: String,
        val width: Int,
        val height: Int
    ) : OutgoingMessageAttachment {
        init {
            require(id.isNotBlank())
            require(bytes.isNotEmpty())
            require(mimeType.startsWith("image/"))
            require(width > 0 && height > 0)
            require(bytes.size <= MessageAttachmentPolicy.MAX_IMAGE_BYTES)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Image

            if (width != other.width) return false
            if (height != other.height) return false
            if (id != other.id) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = width
            result = 31 * result + height
            result = 31 * result + id.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    data class Video(
        override val id: String,
        override val bytes: ByteArray,
        val mimeType: String,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null
    ) : OutgoingMessageAttachment {
        init {
            require(id.isNotBlank())
            require(bytes.isNotEmpty())
            require(mimeType.startsWith("video/"))
            require((width == null) == (height == null))
            require(width == null || width > 0)
            require(height == null || height > 0)
            require(durationMilliseconds == null || durationMilliseconds >= 0L)
            require(bytes.size.toLong() <= MessageAttachmentPolicy.MAX_VIDEO_BYTES)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Video

            if (width != other.width) return false
            if (height != other.height) return false
            if (durationMilliseconds != other.durationMilliseconds) return false
            if (id != other.id) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = width ?: 0
            result = 31 * result + (height ?: 0)
            result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
            result = 31 * result + id.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    data class File(
        override val id: String,
        override val bytes: ByteArray,
        val mimeType: String,
        val fileName: String
    ) : OutgoingMessageAttachment {
        init {
            require(id.isNotBlank())
            require(bytes.isNotEmpty())
            require(mimeType.isNotBlank())
            require(fileName.isNotBlank())
            require(bytes.size.toLong() <= MessageAttachmentPolicy.MAX_FILE_BYTES)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            if (id != other.id) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (mimeType != other.mimeType) return false
            if (fileName != other.fileName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + fileName.hashCode()
            return result
        }
    }

    data class Voice(
        override val id: String,
        override val bytes: ByteArray,
        val mimeType: String,
        val durationMilliseconds: Long
    ) : OutgoingMessageAttachment {
        init {
            require(id.isNotBlank())
            require(bytes.isNotEmpty())
            require(mimeType.startsWith("audio/"))
            require(durationMilliseconds >= 0L)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Voice

            if (durationMilliseconds != other.durationMilliseconds) return false
            if (id != other.id) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (mimeType != other.mimeType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = durationMilliseconds.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    data class Location(
        override val id: String,
        override val bytes: ByteArray
    ) : OutgoingMessageAttachment {
        init {
            require(id.isNotBlank())
            require(bytes.isNotEmpty())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Location

            if (id != other.id) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Contact(
        override val id: String,
        override val bytes: ByteArray
    ) : OutgoingMessageAttachment {
        init {
            require(id.isNotBlank())
            require(bytes.isNotEmpty())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Contact

            if (id != other.id) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }
}
