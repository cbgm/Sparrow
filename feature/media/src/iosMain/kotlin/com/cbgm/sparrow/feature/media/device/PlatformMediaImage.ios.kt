package com.cbgm.sparrow.feature.media.device

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.PlatformContext
import coil3.request.ImageRequest
import com.cbgm.sparrow.core.ui.component.SparrowImage

@Composable
internal actual fun MediaImage(
    data: ByteArray?,
    cacheKey: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val request =
        remember(data, cacheKey) {
            data?.let {
                ImageRequest.Builder(PlatformContext.INSTANCE)
                    .data(it)
                    .memoryCacheKey(cacheKey)
                    .build()
            }
        }

    SparrowImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
