package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class MessageAttachmentFileDataSource(
    rootDirectory: String,
    savedRootDirectory: String = rootDirectory,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val cacheDirectory: Path = rootDirectory.toPath() / CACHE_DIRECTORY_NAME
    private val savedDirectory: Path = savedRootDirectory.toPath() / SAVED_DIRECTORY_NAME

    init {
        fileSystem.createDirectories(cacheDirectory)
        fileSystem.createDirectories(savedDirectory)
    }

    fun write(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Attachment bytes must not be empty" }
        val fileName = "${IdGenerator.generate(prefix = "message-attachment")}.bin"
        val target = cacheDirectory / fileName
        val temporary = cacheDirectory / "$fileName.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.atomicMove(temporary, target)
        return fileName
    }

    fun read(fileName: String): ByteArray? {
        val path = fileName.toSafeCachePath()
        return if (fileSystem.exists(path)) {
            fileSystem.read(path) { readByteArray() }
        } else {
            null
        }
    }

    fun delete(fileName: String) {
        fileSystem.delete(fileName.toSafeCachePath(), mustExist = false)
    }

    fun saveForContact(
        contactId: String,
        contactName: String,
        attachmentId: String,
        type: MessageAttachmentType,
        mimeType: String,
        originalFileName: String?,
        bytes: ByteArray
    ): String {
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(attachmentId.isNotBlank()) { "Attachment ID must not be blank" }
        require(bytes.isNotEmpty()) { "Attachment bytes must not be empty" }

        val contactDirectory = resolveContactDirectory(contactId, contactName)
        val targetDirectory =
            contactDirectory /
                if (type == MessageAttachmentType.IMAGE || type == MessageAttachmentType.VIDEO) {
                    MEDIA_DIRECTORY_NAME
                } else {
                    FILES_DIRECTORY_NAME
                }
        fileSystem.createDirectories(targetDirectory)

        val originalName =
            originalFileName
                ?.substringAfterLast('/')
                ?.substringAfterLast('\\')
                ?.sanitizeFileName()
                ?.takeIf(String::isNotBlank)
        val extension =
            originalName
                ?.substringAfterLast('.', "")
                ?.takeIf(String::isNotBlank)
                ?: mimeType.defaultExtension()
        val baseName =
            originalName
                ?.substringBeforeLast('.', originalName)
                ?.takeIf(String::isNotBlank)
                ?: attachmentId
        val suffix =
            attachmentId
                .filter(Char::isLetterOrDigit)
                .takeLast(ATTACHMENT_ID_SUFFIX_LENGTH)
                .ifBlank { "media" }
        val maxBaseLength = (MAX_FILE_NAME_LENGTH - suffix.length - extension.length - FILE_NAME_SEPARATOR_LENGTH)
            .coerceAtLeast(1)
        val fileName = "${baseName.take(maxBaseLength)}-$suffix.$extension"
        val target = targetDirectory / fileName

        if (!fileSystem.exists(target)) {
            val temporary = targetDirectory / "$fileName.tmp"
            fileSystem.write(temporary) { write(bytes) }
            fileSystem.atomicMove(temporary, target)
        }
        return target.toString()
    }

    private fun resolveContactDirectory(
        contactId: String,
        contactName: String
    ): Path {
        findExistingContactDirectory(contactId)?.let { return it }

        val baseName = contactName.sanitizeDirectoryName().ifBlank { "Contact" }
        val preferred = savedDirectory / baseName
        val preferredMarker = preferred / CONTACT_ID_MARKER

        if (!fileSystem.exists(preferred)) {
            fileSystem.createDirectories(preferred)
            fileSystem.write(preferredMarker) { writeUtf8(contactId) }
            return preferred
        }

        val existingId =
            if (fileSystem.exists(preferredMarker)) {
                runCatching { fileSystem.read(preferredMarker) { readUtf8() } }.getOrNull()
            } else {
                null
            }
        if (existingId == null || existingId == contactId) {
            if (existingId == null) {
                fileSystem.write(preferredMarker) { writeUtf8(contactId) }
            }
            return preferred
        }

        val suffix = contactId.filter(Char::isLetterOrDigit).takeLast(CONTACT_ID_SUFFIX_LENGTH).ifBlank { "contact" }
        val disambiguated = savedDirectory / "$baseName-$suffix"
        fileSystem.createDirectories(disambiguated)
        val marker = disambiguated / CONTACT_ID_MARKER
        if (!fileSystem.exists(marker)) {
            fileSystem.write(marker) { writeUtf8(contactId) }
        }
        return disambiguated
    }

    private fun findExistingContactDirectory(contactId: String): Path? =
        fileSystem.list(savedDirectory).firstOrNull { candidate ->
            val marker = candidate / CONTACT_ID_MARKER
            fileSystem.exists(marker) &&
                runCatching { fileSystem.read(marker) { readUtf8() } }.getOrNull() == contactId
        }

    private fun String.toSafeCachePath(): Path {
        require(isNotBlank() && '/' !in this && '\\' !in this) {
            "Invalid attachment cache file name"
        }
        return cacheDirectory / this
    }

    private fun String.sanitizeDirectoryName(): String =
        trim()
            .map { character ->
                if (character.isLetterOrDigit() || character in DIRECTORY_SAFE_CHARACTERS) character else '_'
            }.joinToString("")
            .trim(' ', '.', '_')
            .take(MAX_DIRECTORY_NAME_LENGTH)

    private fun String.sanitizeFileName(): String =
        trim()
            .map { character ->
                if (character.isLetterOrDigit() || character in FILE_SAFE_CHARACTERS) character else '_'
            }.joinToString("")
            .trim(' ', '.')
            .take(MAX_FILE_NAME_LENGTH)

    private fun String.defaultExtension(): String =
        when (lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "video/mp4" -> "mp4"
            "video/webm" -> "webm"
            "video/3gpp" -> "3gp"
            "application/pdf" -> "pdf"
            else -> substringAfterLast('/', "bin").takeIf(String::isNotBlank) ?: "bin"
        }

    private companion object {
        const val CACHE_DIRECTORY_NAME = "message-attachments"
        const val SAVED_DIRECTORY_NAME = "Sparrow"
        const val MEDIA_DIRECTORY_NAME = "media"
        const val FILES_DIRECTORY_NAME = "files"
        const val CONTACT_ID_MARKER = ".sparrow-contact-id"
        const val CONTACT_ID_SUFFIX_LENGTH = 8
        const val ATTACHMENT_ID_SUFFIX_LENGTH = 8
        const val MAX_DIRECTORY_NAME_LENGTH = 80
        const val MAX_FILE_NAME_LENGTH = 120
        const val FILE_NAME_SEPARATOR_LENGTH = 2
        val DIRECTORY_SAFE_CHARACTERS = setOf(' ', '-', '_', '(', ')')
        val FILE_SAFE_CHARACTERS = DIRECTORY_SAFE_CHARACTERS + setOf('.')
    }
}
