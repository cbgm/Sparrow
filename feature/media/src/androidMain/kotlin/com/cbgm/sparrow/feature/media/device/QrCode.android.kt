package com.cbgm.sparrow.feature.media.device

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.cbgm.sparrow.core.ui.theme.FunctionalColors

@Composable
actual fun QrCode(
    content: String,
    modifier: Modifier
) {
    val bitmap =
        remember(content) {
            createQrBitmap(content)
        }

    Box(
        modifier =
            modifier
                .background(
                    color = FunctionalColors.QrCodeBackground,
                    shape = MaterialTheme.shapes.medium
                ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null
        )
    }
}

private fun createQrBitmap(
    content: String,
    size: Int = 800
): Bitmap {
    val png = renderQrCode(QrCodeRenderRequest(content = content, size = size))
    return requireNotNull(BitmapFactory.decodeByteArray(png, 0, png.size))
}
