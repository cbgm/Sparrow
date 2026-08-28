package com.cbgm.sparrow.feature.media.device

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
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
        localFilePath: String,
        fileName: String,
        mimeType: String
    ): Result<Unit> =
        runCatching {
            val file = File(localFilePath)
            require(file.isFile) { "File does not exist: $localFilePath" }

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

            val viewIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType.ifBlank { DEFAULT_MIME_TYPE })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            context.startActivity(viewIntent)
        }
}

private const val DEFAULT_MIME_TYPE = "application/octet-stream"
