package com.cbgm.sparrow.feature.identity.device

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

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
    val matrix =
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.MARGIN to 1)
        )

    val bitmap = createBitmap(size, size)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap[x, y] =
                if (matrix[x, y]) {
                    FunctionalColors.QrCodeForeground.toArgb()
                } else {
                    FunctionalColors.QrCodeBackground.toArgb()
                }
        }
    }

    return bitmap
}
