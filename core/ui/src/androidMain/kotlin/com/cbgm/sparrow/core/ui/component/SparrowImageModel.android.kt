package com.cbgm.sparrow.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest

@Composable
internal actual fun rememberSparrowImageModel(
    model: Any?,
    memoryCacheKey: String?
): Any? {
    val context = LocalPlatformContext.current

    return remember(context, model, memoryCacheKey) {
        if (memoryCacheKey == null) {
            model
        } else {
            ImageRequest.Builder(context)
                .data(model)
                .memoryCacheKey(memoryCacheKey)
                .build()
        }
    }
}
