package com.cbgm.sparrow.feature.chats.data.attachment.storage

import com.cbgm.sparrow.core.id.IdGenerator
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class MessageAttachmentFileStorage(
    rootDirectory: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val directory: Path = rootDirectory.toPath() / DIRECTORY_NAME

    init {
        fileSystem.createDirectories(directory)
    }

    fun write(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Attachment bytes must not be empty" }
        val fileName = "${IdGenerator.generate(prefix = "message-attachment")}.bin"
        val target = directory / fileName
        val temporary = directory / "$fileName.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.atomicMove(temporary, target)
        return fileName
    }

    fun read(fileName: String): ByteArray? {
        val path = fileName.toSafePath()
        return if (fileSystem.exists(path)) {
            fileSystem.read(path) { readByteArray() }
        } else {
            null
        }
    }

    fun delete(fileName: String) {
        fileSystem.delete(fileName.toSafePath(), mustExist = false)
    }

    private fun String.toSafePath(): Path {
        require(isNotBlank() && '/' !in this && '\\' !in this) {
            "Invalid attachment cache file name"
        }
        return directory / this
    }

    private companion object {
        const val DIRECTORY_NAME = "message-attachments"
    }
}
