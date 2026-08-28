package com.cbgm.sparrow.feature.media.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.data.model.FileBrowserContentDto
import com.cbgm.sparrow.feature.media.data.model.FileBrowserDirectoryDto
import com.cbgm.sparrow.feature.media.data.model.FileBrowserEntryDto
import com.cbgm.sparrow.feature.media.util.toReadableByteSize
import java.io.File

class AndroidFileBrowserDataSource(
    private val context: Context
) : FileBrowserDataSource {
    override fun hasFileAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    override suspend fun setRootDirectory(reference: String): Result<Unit> =
        runCatching {
            require(reference.isBlank() || resolveInsideStorage(reference).isDirectory) {
                "Directory does not exist"
            }
        }

    override suspend fun getRootDirectory(): Result<FileBrowserDirectoryDto> =
        runCatching {
            requireAccess()
            val root = storageRoot()
            check(root.exists() && root.isDirectory) { "Internal storage is not available" }
            FileBrowserDirectoryDto(
                reference = root.canonicalPath,
                displayName = "Internal storage"
            )
        }

    override suspend fun listDirectory(reference: String): Result<List<FileBrowserEntryDto>> =
        runCatching {
            requireAccess()
            val directory = resolveInsideStorage(reference)
            require(directory.isDirectory) { "Directory does not exist" }

            directory.listFiles()
                ?.asSequence()
                ?.filter { file -> file.canRead() }
                ?.map { file -> file.toFileBrowserEntryDto() }
                ?.sortedWith(
                    compareByDescending<FileBrowserEntryDto> { entry -> entry.isDirectory }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { entry -> entry.displayName }
                )
                ?.toList()
                ?: emptyList()
        }

    override suspend fun readFile(
        reference: String,
        maxByteSize: Long
    ): Result<FileBrowserContentDto> =
        runCatching {
            requireAccess()
            require(maxByteSize > 0L) { "Invalid file size limit" }
            val file = resolveInsideStorage(reference)
            require(file.isFile && file.canRead()) { "File is not readable" }
            val size = file.length()
            require(size <= maxByteSize) {
                "${file.name} is too large (${size.toReadableByteSize()}, maximum ${maxByteSize.toReadableByteSize()})"
            }
            FileBrowserContentDto(
                sourceReference = file.canonicalPath,
                displayName = file.name.ifBlank { "file" },
                mimeType = mimeType(file),
                bytes = file.readBytes()
            )
        }

    private fun requireAccess() {
        check(hasFileAccess()) { "File access permission is required" }
    }

    private fun storageRoot(): File {
        val appExternalDirectory =
            requireNotNull(context.getExternalFilesDir(null)) {
                "Internal storage is not available"
            }.canonicalFile
        val androidDirectory =
            generateSequence(appExternalDirectory, File::getParentFile)
                .firstOrNull { directory -> directory.name == ANDROID_DIRECTORY_NAME }
        return requireNotNull(androidDirectory?.parentFile) {
            "Internal storage root could not be resolved"
        }.canonicalFile
    }

    private fun resolveInsideStorage(reference: String): File {
        require(reference.isNotBlank()) { "File reference is empty" }
        val root = storageRoot()
        val candidate = File(reference).canonicalFile
        val rootPath = root.path.trimEnd(File.separatorChar)
        val candidatePath = candidate.path
        require(candidatePath == rootPath || candidatePath.startsWith(rootPath + File.separator)) {
            "File is outside internal storage"
        }
        return candidate
    }

    private fun File.toFileBrowserEntryDto(): FileBrowserEntryDto {
        val canonical = canonicalFile
        return FileBrowserEntryDto(
            reference = canonical.path,
            sourceReference = canonical.path.takeUnless { isDirectory },
            displayName = name.ifBlank { canonical.path },
            isDirectory = isDirectory,
            byteSize = length().takeUnless { isDirectory },
            mimeType = if (isDirectory) null else mimeType(this)
        )
    }

    private fun mimeType(file: File): String {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }
}

private const val ANDROID_DIRECTORY_NAME = "Android"
