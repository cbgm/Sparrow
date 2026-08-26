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

    fun resolveCacheFilePath(fileName: String): String? {
        val path = fileName.toSafeCachePath()
        return path.toString().takeIf { fileSystem.exists(path) }
    }

    fun delete(fileName: String) {
        fileSystem.delete(fileName.toSafeCachePath(), mustExist = false)
    }

    fun saveForConversation(
        conversationId: String,
        displayName: String,
        attachmentId: String,
        type: MessageAttachmentType,
        mimeType: String,
        bytes: ByteArray
    ): String {
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        require(attachmentId.isNotBlank()) { "Attachment ID must not be blank" }
        require(bytes.isNotEmpty()) { "Attachment bytes must not be empty" }

        val conversationDirectory = resolveConversationDirectory(conversationId, displayName)
        val targetDirectory =
            conversationDirectory /
                if (type == MessageAttachmentType.IMAGE || type == MessageAttachmentType.VIDEO) {
                    MEDIA_DIRECTORY_NAME
                } else {
                    FILES_DIRECTORY_NAME
                }
        fileSystem.createDirectories(targetDirectory)

        val extension = mimeType.defaultExtension()
        val baseName = attachmentId.sanitizeFileName().ifBlank { "attachment" }
        val fileName = "${baseName.take(MAX_FILE_NAME_LENGTH - extension.length - 1)}.$extension"
        val target = targetDirectory / fileName

        if (!fileSystem.exists(target)) {
            val temporary = targetDirectory / "$fileName.tmp"
            fileSystem.write(temporary) { write(bytes) }
            fileSystem.atomicMove(temporary, target)
        }
        return target.toString()
    }

    fun deleteSavedAttachment(
        conversationId: String,
        attachmentId: String
    ) {
        val conversationDirectory = findExistingConversationDirectory(conversationId) ?: return
        val safeId = attachmentId.sanitizeFileName().ifBlank { return }
        listOf(MEDIA_DIRECTORY_NAME, FILES_DIRECTORY_NAME).forEach { child ->
            val directory = conversationDirectory / child
            if (!fileSystem.exists(directory)) return@forEach
            fileSystem.list(directory)
                .filter { candidate -> candidate.name.substringBeforeLast('.', candidate.name) == safeId }
                .forEach { candidate -> fileSystem.delete(candidate, mustExist = false) }
        }
    }

    fun deleteSavedConversation(conversationId: String) {
        findExistingConversationDirectory(conversationId)?.let { directory ->
            fileSystem.deleteRecursively(directory, mustExist = false)
        }
    }

    fun deleteLegacyContactAttachment(
        contactId: String,
        attachmentId: String
    ) {
        val contactDirectory = findLegacyContactDirectory(contactId) ?: return
        val suffix = attachmentId.filter(Char::isLetterOrDigit).takeLast(LEGACY_ATTACHMENT_ID_SUFFIX_LENGTH)
        if (suffix.isBlank()) return
        listOf(MEDIA_DIRECTORY_NAME, FILES_DIRECTORY_NAME).forEach { child ->
            val directory = contactDirectory / child
            if (!fileSystem.exists(directory)) return@forEach
            fileSystem.list(directory)
                .filter { candidate ->
                    candidate.name.substringBeforeLast('.', candidate.name).endsWith("-$suffix")
                }.forEach { candidate -> fileSystem.delete(candidate, mustExist = false) }
        }
    }

    private fun findLegacyContactDirectory(contactId: String): Path? =
        fileSystem.list(savedDirectory).firstOrNull { candidate ->
            val marker = candidate / LEGACY_CONTACT_ID_MARKER
            fileSystem.exists(marker) &&
                runCatching { fileSystem.read(marker) { readUtf8() } }.getOrNull() == contactId
        }

    private fun resolveConversationDirectory(
        conversationId: String,
        displayName: String
    ): Path {
        findExistingConversationDirectory(conversationId)?.let { return it }

        val baseName = displayName.sanitizeDirectoryName().ifBlank { "Conversation" }
        val preferred = savedDirectory / baseName
        val preferredMarker = preferred / CONVERSATION_ID_MARKER

        if (!fileSystem.exists(preferred)) {
            fileSystem.createDirectories(preferred)
            fileSystem.write(preferredMarker) { writeUtf8(conversationId) }
            return preferred
        }

        val existingId =
            if (fileSystem.exists(preferredMarker)) {
                runCatching { fileSystem.read(preferredMarker) { readUtf8() } }.getOrNull()
            } else {
                null
            }
        if (existingId == null || existingId == conversationId) {
            if (existingId == null) {
                fileSystem.write(preferredMarker) { writeUtf8(conversationId) }
            }
            return preferred
        }

        val suffix = conversationId.filter(Char::isLetterOrDigit).takeLast(CONVERSATION_ID_SUFFIX_LENGTH)
            .ifBlank { "conversation" }
        val disambiguated = savedDirectory / "$baseName-$suffix"
        fileSystem.createDirectories(disambiguated)
        val marker = disambiguated / CONVERSATION_ID_MARKER
        if (!fileSystem.exists(marker)) {
            fileSystem.write(marker) { writeUtf8(conversationId) }
        }
        return disambiguated
    }

    private fun findExistingConversationDirectory(conversationId: String): Path? =
        fileSystem.list(savedDirectory).firstOrNull { candidate ->
            val marker = candidate / CONVERSATION_ID_MARKER
            fileSystem.exists(marker) &&
                runCatching { fileSystem.read(marker) { readUtf8() } }.getOrNull() == conversationId
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
        const val CONVERSATION_ID_MARKER = ".sparrow-conversation-id"
        const val CONVERSATION_ID_SUFFIX_LENGTH = 8
        const val LEGACY_CONTACT_ID_MARKER = ".sparrow-contact-id"
        const val LEGACY_ATTACHMENT_ID_SUFFIX_LENGTH = 8
        const val MAX_DIRECTORY_NAME_LENGTH = 80
        const val MAX_FILE_NAME_LENGTH = 120
        val DIRECTORY_SAFE_CHARACTERS = setOf(' ', '-', '_', '(', ')')
        val FILE_SAFE_CHARACTERS = DIRECTORY_SAFE_CHARACTERS + setOf('.')
    }
}
