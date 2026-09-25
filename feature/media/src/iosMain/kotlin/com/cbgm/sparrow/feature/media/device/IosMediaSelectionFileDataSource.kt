package com.cbgm.sparrow.feature.media.device

import com.cbgm.sparrow.feature.media.data.datasource.MediaSelectionFileDataSource
import com.cbgm.sparrow.feature.media.data.model.StoredMediaSelectionPathsDto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.create
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class IosMediaSelectionFileDataSource : MediaSelectionFileDataSource {
    private val manager get() = NSFileManager.defaultManager
    private val folder get() = requireNotNull(manager.temporaryDirectory.URLByAppendingPathComponent("sparrow-pending-media"))
    private var checkedStaleFiles = false

    override suspend fun save(id: String, bytes: ByteArray, previewBytes: ByteArray?): StoredMediaSelectionPathsDto =
        withContext(Dispatchers.Default) {
            require(bytes.isNotEmpty()) { "Selected media cannot be empty" }
            val safeId = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            require(safeId.isNotEmpty()) { "Invalid media ID" }
            check(manager.createDirectoryAtURL(folder, withIntermediateDirectories = true, attributes = null, error = null)) {
                "Cannot create media storage"
            }
            if (!checkedStaleFiles) {
                checkedStaleFiles = true
                val folderPath = requireNotNull(folder.path)
                manager.contentsOfDirectoryAtPath(folderPath, error = null)
                    ?.filterIsInstance<String>()
                    ?.filter { it.endsWith(".content") || it.endsWith(".preview") }
                    ?.forEach { name ->
                        val oldPath = "$folderPath/$name"
                        val modified = manager.attributesOfItemAtPath(oldPath, error = null)
                            ?.get(NSFileModificationDate) as? NSDate
                        if (modified != null && modified.timeIntervalSinceNow < -STALE_FILE_AGE_SECONDS) {
                            manager.removeItemAtPath(oldPath, error = null)
                        }
                    }
            }
            val original = requireNotNull(folder.URLByAppendingPathComponent("$safeId.content"))
            val previewUrl = previewBytes?.let { requireNotNull(folder.URLByAppendingPathComponent("$safeId.preview")) }
            try {
                check(bytes.toData().writeToURL(original, atomically = true)) { "Could not save selected media" }
                if (previewUrl != null) {
                    check(previewBytes!!.toData().writeToURL(previewUrl, atomically = true)) {
                        "Could not save preview"
                    }
                }
            } catch (error: Exception) {
                original.path?.let { manager.removeItemAtPath(it, error = null) }
                previewUrl?.path?.let { manager.removeItemAtPath(it, error = null) }
                throw error
            }
            StoredMediaSelectionPathsDto(requireNotNull(original.path), previewUrl?.path)
        }

    override suspend fun read(localFilePath: String): ByteArray = withContext(Dispatchers.Default) {
        val data = requireNotNull(manager.contentsAtPath(validate(localFilePath))) { "Selected media is unavailable" }
        val length = data.length.toInt()
        if (length == 0) {
            byteArrayOf()
        } else {
            ByteArray(length).also { output ->
                output.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
            }
        }
    }

    override suspend fun delete(localFilePath: String) = withContext(Dispatchers.Default) {
        val path = validate(localFilePath)
        if (manager.fileExistsAtPath(path)) {
            check(manager.removeItemAtPath(path, error = null)) {
                "Could not delete temporary media"
            }
        }
    }

    private companion object {
        const val STALE_FILE_AGE_SECONDS = 7.0 * 24 * 60 * 60
    }

    private fun validate(path: String): String {
        val prefix = requireNotNull(folder.path).trimEnd('/') + "/"
        require(path.startsWith(prefix) && !path.removePrefix(prefix).contains('/')) { "Invalid selected media path" }
        return path
    }

    private fun ByteArray.toData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
