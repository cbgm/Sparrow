package com.cbgm.sparrow.feature.media.device

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import org.jetbrains.skia.Image as SkiaImage

@Composable
actual fun QrCode(content: String, modifier: Modifier) {
    val bitmap = remember(content) {
        SkiaImage.makeFromEncoded(renderQrCode(QrCodeRenderRequest(content = content))).toComposeImageBitmap()
    }
    Box(
        modifier = modifier.background(
            color = FunctionalColors.QrCodeBackground,
            shape = MaterialTheme.shapes.medium
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(bitmap = bitmap, contentDescription = null)
    }
}
