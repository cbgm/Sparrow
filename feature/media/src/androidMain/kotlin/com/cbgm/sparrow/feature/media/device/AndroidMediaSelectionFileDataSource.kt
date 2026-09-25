package com.cbgm.sparrow.feature.media.device

import android.content.Context
import com.cbgm.sparrow.feature.media.data.datasource.MediaSelectionFileDataSource
import com.cbgm.sparrow.feature.media.data.model.StoredMediaSelectionPathsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidMediaSelectionFileDataSource(
    private val context: Context
) : MediaSelectionFileDataSource {
    private val directory: File get() = File(context.filesDir, "pending-media")
    private var checkedStaleFiles = false

    override suspend fun save(id: String, bytes: ByteArray, previewBytes: ByteArray?): StoredMediaSelectionPathsDto =
        withContext(Dispatchers.IO) {
            require(bytes.isNotEmpty()) { "Selected media cannot be empty" }
            val safeId = id.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            require(safeId.isNotEmpty()) { "Invalid media ID" }
            val folder = directory.apply { check(isDirectory || mkdirs()) { "Cannot create media storage" } }
            // Once per process: reclaim copies left after a process kill. A generous
            // retention window avoids deleting drafts still open in another conversation.
            if (!checkedStaleFiles) {
                checkedStaleFiles = true
                val expiry = System.currentTimeMillis() - STALE_FILE_AGE_MS
                folder.listFiles()?.forEach { old ->
                    if (old.isFile && old.lastModified() in 1L until expiry &&
                        (old.name.endsWith(".content") || old.name.endsWith(".preview"))
                    ) {
                        old.delete() // Best effort: cleanup must not prevent selecting new media.
                    }
                }
            }
            val original = File(folder, "$safeId.content")
            val preview = previewBytes?.let { File(folder, "$safeId.preview") }
            try {
                original.writeBytes(bytes)
                if (preview != null) preview.writeBytes(previewBytes)
            } catch (error: Exception) {
                original.delete()
                preview?.delete()
                throw error
            }
            StoredMediaSelectionPathsDto(original.absolutePath, preview?.absolutePath)
        }

    override suspend fun read(localFilePath: String): ByteArray = withContext(Dispatchers.IO) {
        val file = requirePrivateFile(localFilePath)
        require(file.isFile) { "Selected media is no longer available" }
        file.readBytes()
    }

    override suspend fun delete(localFilePath: String) = withContext(Dispatchers.IO) {
        val file = requirePrivateFile(localFilePath)
        if (file.exists()) check(file.delete()) { "Could not delete temporary media" }
    }

    private companion object {
        const val STALE_FILE_AGE_MS = 7L * 24 * 60 * 60 * 1_000
    }

    private fun requirePrivateFile(path: String): File {
        val folder = directory.canonicalFile
        val file = File(path).canonicalFile
        require(file.parentFile == folder) { "Invalid selected media path" }
        return file
    }
}
