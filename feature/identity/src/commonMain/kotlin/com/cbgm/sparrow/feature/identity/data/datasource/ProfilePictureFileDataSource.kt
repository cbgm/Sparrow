package com.cbgm.sparrow.feature.identity.data.datasource

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class ProfilePictureFileDataSource(
    rootDirectory: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {
    private val root: Path = rootDirectory.toPath()
    private val remoteDirectory: Path = root / REMOTE_DIRECTORY
    private val localPicture: Path = root / LOCAL_FILE_NAME

    init {
        fileSystem.createDirectories(root)
        fileSystem.createDirectories(remoteDirectory)
    }

    fun readLocal(): ByteArray? = read(localPicture)

    fun writeLocal(bytes: ByteArray) {
        writeAtomically(localPicture, bytes)
    }

    fun deleteLocal() {
        fileSystem.delete(localPicture, mustExist = false)
    }

    fun readRemote(fileName: String): ByteArray? = read(remoteDirectory / fileName)

    fun writeRemote(
        fileName: String,
        bytes: ByteArray
    ) {
        writeAtomically(remoteDirectory / fileName, bytes)
    }

    fun deleteRemote(fileName: String) {
        fileSystem.delete(remoteDirectory / fileName, mustExist = false)
    }

    private fun read(path: Path): ByteArray? =
        if (fileSystem.exists(path)) {
            fileSystem.read(path) { readByteArray() }
        } else {
            null
        }

    private fun writeAtomically(
        target: Path,
        bytes: ByteArray
    ) {
        val temporary = target.parent!! / "${target.name}.tmp"
        fileSystem.write(temporary) { write(bytes) }
        fileSystem.delete(target, mustExist = false)
        fileSystem.atomicMove(temporary, target)
    }

    private companion object {
        const val LOCAL_FILE_NAME = "profile-picture.jpg"
        const val REMOTE_DIRECTORY = "remote-profile-pictures"
    }
}
