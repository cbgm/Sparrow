package com.cbgm.sparrow.feature.media.device

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.model.MediaExportItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberMediaExporter(): MediaExporter {
    val context = LocalContext.current
    return remember(context) { AndroidMediaExporter(context.applicationContext) }
}

private class AndroidMediaExporter(
    private val context: Context
) : MediaExporter {
    override suspend fun saveToCameraRoll(media: List<MediaExportItem>): Result<Int> =
        runCatching {
            withContext(Dispatchers.IO) {
                media.count { item -> saveIfMissing(item) }
            }
        }

    private fun saveIfMissing(item: MediaExportItem): Boolean {
        val collection =
            when (item.type) {
                MediaContentType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MediaContentType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        val relativePath =
            when (item.type) {
                MediaContentType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/Sparrow"
                MediaContentType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/Sparrow"
            }
        val displayName = "sparrow-${item.id}.${item.mimeType.defaultExtension()}"
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
