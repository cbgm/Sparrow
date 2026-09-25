package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import com.cbgm.sparrow.core.ui.component.SparrowImage
import java.io.File

@Composable
internal actual fun MediaImage(
    data: ByteArray?,
    localFilePath: String?,
    cacheKey: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val request =
        remember(data, localFilePath, cacheKey, context) {
            val source = localFilePath?.let(::File) ?: data ?: return@remember null
            ImageRequest.Builder(context)
                .data(source)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .build()
        }

    SparrowImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
