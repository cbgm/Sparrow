package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.divider

/** Three stops on a path: sent, delivered, read. The stops reached so far are cyan. */
@Composable
fun SparrowDeliveryDots(
    reached: Int,
    description: String,
    modifier: Modifier = Modifier
) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outlineVariant
    val path = MaterialTheme.colorScheme.divider
    Canvas(
        modifier = modifier
            .size(width = Dimens.MessageBubble.deliveryDotsWidth, height = Dimens.MessageBubble.deliveryDotsHeight)
            .semantics { contentDescription = description }
    ) {
        val radius = Dimens.MessageBubble.deliveryDotRadius.toPx()
        val stroke = Dimens.MessageBubble.deliveryLineWidth.toPx()
        val centerY = size.height / 2f
        val step = (size.width - radius * 2f) / 2f
        val stops = List(3) { index -> Offset(radius + step * index, centerY) }
        drawLine(color = path, start = stops.first(), end = stops.last(), strokeWidth = stroke)
        if (reached > 1) {
            drawLine(color = on, start = stops.first(), end = stops[reached - 1], strokeWidth = stroke)
        }
        stops.forEachIndexed { index, center ->
            drawCircle(color = if (index < reached) on else off, radius = radius, center = center)
        }
    }
}
