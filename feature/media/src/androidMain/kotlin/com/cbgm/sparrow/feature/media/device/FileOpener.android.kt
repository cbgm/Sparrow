package com.cbgm.sparrow.feature.media.device

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberFileOpener(): FileOpener {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidFileOpener(context) }
}

private class AndroidFileOpener(
    private val context: Context
) : FileOpener {
    override suspend fun open(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<Unit> =
        runCatching {
            require(fileName.isNotBlank()) { "File name must not be blank" }
            require(bytes.isNotEmpty()) { "File is empty" }

            val sharedFile =
                withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, SHARED_FILE_DIRECTORY).apply { mkdirs() }
                    File(directory, fileName.safeFileName()).also { file ->
                        file.outputStream().use { output -> output.write(bytes) }
                    }
                }

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.sparrow.media.fileprovider",
                    sharedFile
                )
            val viewIntent =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mimeType.ifBlank { DEFAULT_MIME_TYPE })
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val chooser =
                Intent.createChooser(viewIntent, null)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        }
}

private fun String.safeFileName(): String {
    val sanitized =
        replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(MAX_FILE_NAME_LENGTH)
    return sanitized.ifBlank { "sparrow-file" }
}

private const val SHARED_FILE_DIRECTORY = "shared_files"
private const val DEFAULT_MIME_TYPE = "application/octet-stream"
private const val MAX_FILE_NAME_LENGTH = 180
