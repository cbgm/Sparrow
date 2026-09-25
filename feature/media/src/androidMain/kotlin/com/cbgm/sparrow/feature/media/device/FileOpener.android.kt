package com.cbgm.sparrow.feature.media.device

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.cbgm.sparrow.core.result.safeSuspendCall
import java.io.File

@Composable
actual fun rememberFileOpener(): FileOpener {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        AndroidFileOpener(context)
    }
}

private class AndroidFileOpener(
    private val context: Context
) : FileOpener {
    override suspend fun open(
        localFilePath: String,
        fileName: String,
        mimeType: String
    ): Result<Unit> =
        safeSuspendCall {
            val file = File(localFilePath)

            require(file.isFile) {
                "File does not exist: $localFilePath"
            }

            openFile(
                file = file,
                mimeType = mimeType
            )
        }

    override suspend fun open(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<Unit> =
        safeSuspendCall {
            require(bytes.isNotEmpty()) {
                "File bytes must not be empty"
            }

            val file =
                File.createTempFile(
                    TEMP_FILE_PREFIX,
                    fileName.extensionSuffix(),
                    context.cacheDir
                ).apply {
                    writeBytes(bytes)
                }

            openFile(
                file = file,
                mimeType = mimeType
            )
        }

    private fun openFile(
        file: File,
        mimeType: String
    ) {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uri,
                    mimeType.ifBlank { DEFAULT_MIME_TYPE }
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        context.startActivity(intent)
    }
}

private fun String.extensionSuffix(): String {
    val extension = substringAfterLast('.', missingDelimiterValue = "")
    return extension
        .takeIf(String::isNotBlank)
        ?.let { ".$it" }
        .orEmpty()
}

private const val TEMP_FILE_PREFIX = "sparrow-"
private const val DEFAULT_MIME_TYPE = "application/octet-stream"
