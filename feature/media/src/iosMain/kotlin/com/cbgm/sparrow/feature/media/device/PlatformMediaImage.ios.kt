package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.PlatformContext
import coil3.request.ImageRequest
import com.cbgm.sparrow.core.ui.component.SparrowImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun MediaImage(
    data: ByteArray?,
    localFilePath: String?,
    cacheKey: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val request =
        remember(data, localFilePath, cacheKey) {
            val source =
                localFilePath
                    ?.let { path -> NSURL.fileURLWithPath(path).absoluteString }
                    ?: data
                    ?: return@remember null
            ImageRequest.Builder(PlatformContext.INSTANCE)
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
