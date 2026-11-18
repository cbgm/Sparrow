package com.cbgm.sparrow.feature.chats.presentation.common.composer.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.messageBubble
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
internal fun TypingIndicator(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.messageBubble.theirs,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.base
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            TypingDots()
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typingDots")
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TypingIndicator.dotGap)) {
        repeat(DotCount) { index ->
            val alpha by transition.animateFloat(
                initialValue = MinAlpha,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(PulseMillis),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * DotDelayMillis)
                    ),
                label = "typingDot"
            )
            Box(
                modifier =
                    Modifier
                        .size(Dimens.TypingIndicator.dotSize)
                        .alpha(alpha)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            )
        }
    }
}

private const val DotCount = 3
private const val PulseMillis = 450
private const val DotDelayMillis = 150
private const val MinAlpha = 0.25f

@Preview
@Composable
private fun TypingIndicatorPreview() {
    SparrowTheme {
        TypingIndicator(text = "Chris schreibt…")
    }
}
