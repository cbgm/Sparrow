package com.cbgm.sparrow.feature.chats.data.group.datasource

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class GroupAvatarFileDataSource(
    rootDirectory: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val directory: Path = rootDirectory.toPath() / GROUP_AVATAR_DIRECTORY

    init {
        fileSystem.createDirectories(directory)
    }

    fun read(fileName: String): ByteArray? {
        val path = directory / fileName
        return if (fileSystem.exists(path)) {
            fileSystem.read(path) { readByteArray() }
        } else {
            null
        }
    }

    fun write(
        fileName: String,
        bytes: ByteArray
    ) {
        require(bytes.isNotEmpty()) { "Group avatar must not be empty" }
        val target = directory / fileName
        val temporary = directory / "$fileName.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.delete(target, mustExist = false)
        fileSystem.atomicMove(temporary, target)
    }

    fun delete(fileName: String) {
        fileSystem.delete(directory / fileName, mustExist = false)
    }

    private companion object {
        const val GROUP_AVATAR_DIRECTORY = "group-avatars"
    }
}
