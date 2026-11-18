package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing

/**
 * The main way on: a word and a thin cyan line. Pressing shows a soft cyan ripple in a rounded pill around both, and
 * the line stretches. The pill reaches into the margin, so the word stays aligned with the text around it.
 */
@Composable
fun SparrowAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lineWidth by animateDpAsState(
        if (pressed) Dimens.Button.actionLinePressedWidth else Dimens.Button.actionLineWidth,
        label = "actionLine"
    )
    Row(
        modifier =
            modifier
                .bleedHorizontally(Dimens.Button.actionPadding)
                .clip(Pill)
                .clickable(
                    interactionSource = interaction,
                    indication = sparrowRipple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
                .heightIn(min = Dimens.Button.minTouchHeight)
                .padding(horizontal = Dimens.Button.actionPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline
        )
        Box(
            Modifier
                .size(lineWidth, Dimens.Button.actionLineThickness)
                .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        )
    }
}

/** A small cyan text link ("Why is that?"), with the same rounded ripple as [SparrowAction] and no line. */
@Composable
fun SparrowLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .bleedHorizontally(Dimens.Button.linkPadding)
                .clip(Pill)
                .clickable(interactionSource = null, indication = sparrowRipple(), role = Role.Button, onClick = onClick)
                .padding(horizontal = Dimens.Button.linkPadding, vertical = MaterialTheme.spacing.base)
    )
}

/** The press feedback: the platform ripple, tinted cyan. */
@Composable
fun sparrowRipple(): Indication = ripple(color = MaterialTheme.colorScheme.primary)

/**
 * Lets the content (and its ripple) reach [amount] into the space left and right of it, without moving it: a row that
 * spans the page can ripple from edge to edge, a word keeps its alignment while its pill reaches into the margin.
 */
fun Modifier.bleedHorizontally(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (amount * 2).roundToPx()
        val placeable = measurable.measure(constraints.offset(horizontal = extra))
        layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
    }

private val Pill = RoundedCornerShape(percent = 50)

@Preview
@Composable
private fun SparrowActionPreview() {
    SparrowTheme {
        Column {
            SparrowAction(text = "Allow", onClick = {})
            SparrowAction(text = "Create", onClick = {}, enabled = false)
            SparrowLink(text = "Why is that?", onClick = {})
        }
    }
}
