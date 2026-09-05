package com.cbgm.sparrow.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.PlatformContext
import coil3.request.ImageRequest

@Composable
internal actual fun rememberSparrowImageModel(
    model: Any?,
    memoryCacheKey: String?
): Any? =
    remember(model, memoryCacheKey) {
        if (memoryCacheKey == null) {
            model
        } else {
            ImageRequest.Builder(PlatformContext.INSTANCE)
                .data(model)
                .memoryCacheKey(memoryCacheKey)
                .build()
        }
    }
