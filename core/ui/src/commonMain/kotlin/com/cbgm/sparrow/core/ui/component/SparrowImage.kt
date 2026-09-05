package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.cbgm.sparrow.core.ui.theme.FunctionalColors
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

/**
 * Shared Sparrow image loader.
 *
 * Callers own layout, clipping and shape. This component only owns image loading/decoding,
 * caching and the common load/error fallback.
 */

private const val FALLBACK_BITMAP_SIZE = 48
private const val FALLBACK_INSET = 13f
private const val FALLBACK_STROKE_WIDTH = 4f

@Composable
fun SparrowImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    error: Painter? = null,
    memoryCacheKey: String? = null,
    showLoadingBackground: Boolean = true
) {
    val fallbackPainter = error ?: rememberSparrowFallbackPainter()
    val imageModel = rememberSparrowImageModel(model, memoryCacheKey)
    val imageModifier =
        if (showLoadingBackground) {
            modifier.background(FunctionalColors.MediaBackground)
        } else {
            modifier
        }

    AsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        modifier = imageModifier,
        contentScale = contentScale,
        alignment = alignment,
        placeholder = null,
        error = fallbackPainter,
        fallback = null
    )
}

@Composable
internal expect fun rememberSparrowImageModel(
    model: Any?,
    memoryCacheKey: String?
): Any?

@Composable
fun rememberSparrowFallbackPainter(): Painter {
    val background = FunctionalColors.MediaBackground
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant

    return remember(background, foreground) {
        val bitmap = ImageBitmap(FALLBACK_BITMAP_SIZE, FALLBACK_BITMAP_SIZE)
        val canvas = Canvas(bitmap)
        val backgroundPaint = Paint().apply { color = background }
        val foregroundPaint =
            Paint().apply {
                color = foreground
                strokeWidth = FALLBACK_STROKE_WIDTH
            }

        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = FALLBACK_BITMAP_SIZE.toFloat(),
            bottom = FALLBACK_BITMAP_SIZE.toFloat(),
            paint = backgroundPaint
        )
        canvas.drawLine(
            p1 = Offset(FALLBACK_INSET, FALLBACK_INSET),
            p2 = Offset(FALLBACK_BITMAP_SIZE - FALLBACK_INSET, FALLBACK_BITMAP_SIZE - FALLBACK_INSET),
            paint = foregroundPaint
        )
        canvas.drawLine(
            p1 = Offset(FALLBACK_BITMAP_SIZE - FALLBACK_INSET, FALLBACK_INSET),
            p2 = Offset(FALLBACK_INSET, FALLBACK_BITMAP_SIZE - FALLBACK_INSET),
            paint = foregroundPaint
        )

        BitmapPainter(bitmap)
    }
}

@Preview
@Composable
private fun SparrowImagePreview() {
    SparrowTheme {
        SparrowImage(
            model = byteArrayOf(),
            contentDescription = null
        )
    }
}
