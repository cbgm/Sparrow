package com.cbgm.sparrow.feature.media.device

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.data.model.FileBrowserContentData
import com.cbgm.sparrow.feature.media.data.model.FileBrowserDirectoryData
import com.cbgm.sparrow.feature.media.data.model.FileBrowserEntryData
import com.cbgm.sparrow.feature.media.util.toReadableByteSize
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
class IosFileBrowserDataSource : FileBrowserDataSource {
    private var rootUrl: NSURL? = null

    override fun hasFileAccess(): Boolean = rootUrl != null

    override suspend fun setRootDirectory(reference: String): Result<Unit> =
        runCatching {
            val selectedUrl = requireNotNull(NSURL(string = reference)) {
                "Selected directory reference is invalid"
            }
            val path = requireNotNull(selectedUrl.path) { "Selected directory has no path" }
            check(NSFileManager.defaultManager.fileExistsAtPath(path)) {
                "Selected directory is not available"
            }
            check(selectedUrl.startAccessingSecurityScopedResource()) {
                "Selected directory access was not granted"
            }

            rootUrl?.stopAccessingSecurityScopedResource()
            rootUrl = selectedUrl
        }

    override suspend fun getRootDirectory(): Result<FileBrowserDirectoryData> =
        runCatching {
            val root = requireRootUrl()
            FileBrowserDirectoryData(
                reference = requireNotNull(root.path),
                displayName = root.lastPathComponent?.takeIf(String::isNotBlank) ?: "Files"
            )
        }

    override suspend fun listDirectory(reference: String): Result<List<FileBrowserEntryData>> =
        runCatching {
            val directoryPath = resolveInsideRoot(reference)
            val manager = NSFileManager.defaultManager
            val names =
                manager.contentsOfDirectoryAtPath(directoryPath, error = null)
                    ?.filterIsInstance<String>()
                    .orEmpty()

            names.mapNotNull { name ->
                val childPath = directoryPath.trimEnd('/') + "/" + name
                val attributes = manager.attributesOfItemAtPath(childPath, error = null) ?: return@mapNotNull null
                val isDirectory = attributes[NSFileType] == NSFileTypeDirectory
                FileBrowserEntryData(
                    reference = childPath,
                    sourceReference = childPath.takeUnless { isDirectory },
                    displayName = name,
                    isDirectory = isDirectory,
                    byteSize = (attributes[NSFileSize] as? NSNumber)?.longLongValue?.takeUnless { isDirectory },
                    mimeType = if (isDirectory) null else mimeType(name)
                )
            }.sortedWith(
                compareByDescending<FileBrowserEntryData> { entry -> entry.isDirectory }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { entry -> entry.displayName }
            )
        }

    override suspend fun readFile(
        reference: String,
        maxByteSize: Long
    ): Result<FileBrowserContentData> =
        runCatching {
            require(maxByteSize > 0L) { "Invalid file size limit" }
            val path = resolveInsideRoot(reference)
            val manager = NSFileManager.defaultManager
            val attributes = requireNotNull(manager.attributesOfItemAtPath(path, error = null)) {
                "File is not readable"
            }
            require(attributes[NSFileType] != NSFileTypeDirectory) { "Directories cannot be selected" }
            val byteSize = (attributes[NSFileSize] as? NSNumber)?.longLongValue ?: 0L
            require(byteSize <= maxByteSize) {
                "${path.substringAfterLast('/')} is too large " +
                    "(${byteSize.toReadableByteSize()}, maximum ${maxByteSize.toReadableByteSize()})"
            }
            val bytes = requireNotNull(manager.contentsAtPath(path)) { "File is not readable" }.toByteArray()
            require(bytes.size.toLong() <= maxByteSize) { "File exceeds the maximum size" }

            FileBrowserContentData(
                sourceReference = path,
                displayName = path.substringAfterLast('/').ifBlank { "file" },
                mimeType = mimeType(path),
                bytes = bytes
            )
        }

    private fun requireRootUrl(): NSURL =
        requireNotNull(rootUrl) { "File access is required" }

    private fun resolveInsideRoot(reference: String): String {
        require(reference.isNotBlank()) { "File reference is empty" }
        val rootPath = requireNotNull(requireRootUrl().path).trimEnd('/')
        require(reference == rootPath || reference.startsWith("$rootPath/")) {
            "File is outside the selected directory"
        }
        require(NSFileManager.defaultManager.fileExistsAtPath(reference)) {
            "File or directory does not exist"
        }
        return reference
    }

    private fun mimeType(path: String): String =
        when (path.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
            "pdf" -> "application/pdf"
            "txt", "log" -> "text/plain"
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "heic" -> "image/heic"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
}
