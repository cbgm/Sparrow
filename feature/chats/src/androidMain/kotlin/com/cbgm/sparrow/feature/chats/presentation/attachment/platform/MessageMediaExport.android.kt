package com.cbgm.sparrow.feature.chats.presentation.attachment.platform

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.feature.chats.domain.model.attachment.MessageMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMessageMediaExporter(): MessageMediaExporter {
    val context = LocalContext.current
    return remember(context) { AndroidMessageMediaExporter(context.applicationContext) }
}

private class AndroidMessageMediaExporter(
    private val context: Context
) : MessageMediaExporter {
    override suspend fun saveToCameraRoll(media: List<MessageMediaExportItem>): Result<Int> =
        runCatching {
            withContext(Dispatchers.IO) {
                media.count { item -> saveIfMissing(item) }
            }
        }

    private fun saveIfMissing(item: MessageMediaExportItem): Boolean {
        val collection =
            when (item.type) {
                MessageMediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MessageMediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val relativePath =
            when (item.type) {
                MessageMediaType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/Sparrow"
                MessageMediaType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/Sparrow"
            }
        val displayName = "sparrow-${item.attachmentId}.${item.mimeType.defaultExtension()}"
        val resolver = context.contentResolver
        if (resolver.contains(collection, displayName, relativePath)) return false

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri = resolver.insert(collection, values) ?: error("Could not create camera-roll item")
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> output.write(item.bytes) }
                ?: error("Could not open camera-roll item")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null
            )
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
        return true
    }

    private fun android.content.ContentResolver.contains(
        collection: Uri,
        displayName: String,
        relativePath: String
    ): Boolean =
        query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(displayName, "$relativePath/"),
            null
        )?.use { cursor -> cursor.moveToFirst() } == true
}

private fun String.defaultExtension(): String =
    when (lowercase()) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "video/mp4" -> "mp4"
        "video/webm" -> "webm"
        "video/3gpp" -> "3gp"
        else -> substringAfterLast('/', "bin").takeIf(String::isNotBlank) ?: "bin"
    }
