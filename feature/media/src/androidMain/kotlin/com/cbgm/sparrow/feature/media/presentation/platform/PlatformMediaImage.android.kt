package com.cbgm.sparrow.feature.media.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import com.cbgm.sparrow.core.ui.component.SparrowImage

@Composable
internal actual fun PlatformMediaImage(
    data: ByteArray?,
    cacheKey: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val request =
        remember(data, cacheKey, context) {
            data?.let {
                ImageRequest.Builder(context)
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
