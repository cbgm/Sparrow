package com.cbgm.sparrow.feature.media.device

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream

actual fun renderQrCode(request: QrCodeRenderRequest): ByteArray {
    val qr = encodeQr(request)
    try {
        if (request.caption.isEmpty()) return qr.toPng()
        val layout = QrCodeImageLayout(qr.width)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = layout.textSize.toFloat()
        }
        val lines = layout.wrap(request.caption) { paint.measureText(it).toDouble() }
        val image = createBitmap(layout.width, layout.height(lines.size))
        try {
            val canvas = Canvas(image)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(qr, layout.qrLeft.toFloat(), layout.qrTop.toFloat(), null)
            lines.forEachIndexed { index, line ->
                val baseline = layout.textTop + index * layout.lineHeight - paint.fontMetrics.ascent
                canvas.drawText(line, layout.textLeft.toFloat(), baseline, paint)
            }
            return image.toPng()
        } finally {
            image.recycle()
        }
    } finally {
        qr.recycle()
    }
}

private fun encodeQr(request: QrCodeRenderRequest): Bitmap {
    val correction = when (request.errorCorrection) {
        QrErrorCorrection.LOW -> ErrorCorrectionLevel.L
        QrErrorCorrection.MEDIUM -> ErrorCorrectionLevel.M
        QrErrorCorrection.QUARTILE -> ErrorCorrectionLevel.Q
        QrErrorCorrection.HIGH -> ErrorCorrectionLevel.H
    }
    val matrix = QRCodeWriter().encode(
        request.content,
        BarcodeFormat.QR_CODE,
        request.size,
        request.size,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to correction,
            EncodeHintType.MARGIN to request.margin
        )
    )
    require(matrix.width == request.size && matrix.height == request.size) { "QR image size is too small" }
    val foreground = FunctionalColors.QrCodeForeground.toArgb()
    val background = FunctionalColors.QrCodeBackground.toArgb()
    val bitmap = createBitmap(matrix.width, matrix.height)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix[index % matrix.width, index / matrix.width]) foreground else background
    }
    bitmap.setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    return bitmap
}

private fun Bitmap.toPng(): ByteArray = ByteArrayOutputStream().use { output ->
    check(compress(Bitmap.CompressFormat.PNG, 100, output)) { "QR image encoding failed" }
    output.toByteArray()
}
