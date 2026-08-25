package com.cbgm.sparrow.feature.media.presentation.avatar

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun ProfilePictureImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val imageBitmap =
        remember(bytes) {
            runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        }

    imageBitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
