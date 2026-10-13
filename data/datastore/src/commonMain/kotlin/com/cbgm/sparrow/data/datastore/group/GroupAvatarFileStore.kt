package com.cbgm.sparrow.data.datastore.group

import com.cbgm.sparrow.core.crypto.hash.CryptoHash
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** Group avatar image files. The file name is derived from the group ID so it never leaks the ID itself. */
class GroupAvatarFileStore(
    rootDirectory: String,
    private val cryptoHash: CryptoHash,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val directory: Path = rootDirectory.toPath() / GROUP_AVATAR_DIRECTORY

    init {
        fileSystem.createDirectories(directory)
    }

    fun exists(groupId: String): Boolean = fileSystem.exists(pathOf(groupId))

    fun read(groupId: String): ByteArray? {
        val path = pathOf(groupId)
        return if (fileSystem.exists(path)) {
            fileSystem.read(path) { readByteArray() }
        } else {
            null
        }
    }

    fun write(groupId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Group avatar must not be empty" }
        val target = pathOf(groupId)
        val temporary = directory / "${target.name}.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.delete(target, mustExist = false)
        fileSystem.atomicMove(temporary, target)
    }

    fun delete(groupId: String) {
        fileSystem.delete(pathOf(groupId), mustExist = false)
    }

    private fun pathOf(groupId: String): Path {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        val hash =
            cryptoHash
                .sha256(groupId.encodeToByteArray())
                .joinToString(separator = "") { byte ->
                    (byte.toInt() and 0xff).toString(radix = 16).padStart(2, '0')
                }
        return directory / "$hash.jpg"
    }

    private companion object {
        const val GROUP_AVATAR_DIRECTORY = "group-avatars"
    }
}
