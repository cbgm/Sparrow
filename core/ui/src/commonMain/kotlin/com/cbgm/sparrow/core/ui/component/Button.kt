package com.cbgm.sparrow.core.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.asTint
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun SparrowApprovalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TintedButton(
        onClick = onClick,
        modifier = modifier,
        text = text,
        fillMaxWidth = fillMaxWidth,
        content = content,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
fun SparrowDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TintedButton(
        onClick = onClick,
        modifier = modifier,
        text = text,
        fillMaxWidth = fillMaxWidth,
        content = content,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.error.asTint(Alpha.Button.destructive),
        contentColor = MaterialTheme.colorScheme.error
    )
}

@Composable
fun SparrowBannerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    textColor: Color = LocalContentColor.current,
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                contentColor = textColor,
                containerColor = Color.Transparent
            ),
        modifier =
            modifier.then(
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (content != {}) {
            content()
        }
    }
}

@Composable
fun SparrowSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TintedButton(
        onClick = onClick,
        modifier = modifier,
        text = text,
        fillMaxWidth = fillMaxWidth,
        content = content,
        enabled = enabled,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun SparrowOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit = {},
    enabled: Boolean = true
) {
    TintedButton(
        onClick = onClick,
        modifier = modifier,
        text = text,
        fillMaxWidth = fillMaxWidth,
        content = content,
        enabled = enabled,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun TintedButton(
    onClick: () -> Unit,
    modifier: Modifier,
    text: String,
    fillMaxWidth: Boolean,
    content: @Composable () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null
) {
    val alpha = if (enabled) 1f else Alpha.Button.disabled
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.Button.cornerRadius),
        border = borderColor?.let { color -> BorderStroke(Dimens.Button.borderWidth, color.copy(alpha = color.alpha * alpha)) },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = containerColor.alpha * Alpha.Button.disabled),
                disabledContentColor = contentColor.copy(alpha = Alpha.Button.disabled)
            ),
        elevation = null,
        modifier =
            modifier
                .defaultMinSize(minHeight = Dimens.Button.height)
                .then(
                    if (fillMaxWidth) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                    }
                )
    ) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        content()
    }
}

@Composable
fun SparrowRoundApprovalButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.circle,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Button.disabled),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = Alpha.Button.disabled)
        ),
        elevation = null,
        contentPadding = PaddingValues(MaterialTheme.spacing.button.iconContentPadding),
        modifier = modifier.size(Dimens.Button.iconButtonSize),
        enabled = enabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null
        )
    }
}

@Composable
fun SparrowHoldAction(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBusy: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector = SparrowIcons.Check
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val active = enabled && !isBusy
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        Box(
            modifier =
                Modifier
                    .size(Dimens.HoldAction.size)
                    .semantics {
                        role = Role.Button
                        if (active) {
                            onLongClick(label = label) {
                                currentOnConfirm()
                                true
                            }
                        }
                    }.pointerInput(active) {
                        if (!active) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                val fill =
                                    scope.launch {
                                        progress.animateTo(
                                            targetValue = 1f,
                                            animationSpec = tween(HoldMillis, easing = LinearEasing)
                                        )
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentOnConfirm()
                                        progress.snapTo(0f)
                                    }
                                tryAwaitRelease()
                                if (fill.isActive) {
                                    fill.cancel()
                                    scope.launch { progress.animateTo(0f, tween(ReleaseMillis)) }
                                }
                            }
                        )
                    }.drawBehind { drawRings(trackColor = trackColor, color = color, progress = progress.value) },
            contentAlignment = Alignment.Center
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.HoldAction.iconSize),
                    strokeWidth = Dimens.Base.progressIndicatorStrokeWidth,
                    color = color
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.HoldAction.iconSize),
                    tint = contentColor
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor
        )
    }
}

private fun DrawScope.drawRings(
    trackColor: Color,
    color: Color,
    progress: Float
) {
    val outerStroke = Dimens.HoldAction.outerStroke.toPx()
    val innerStroke = Dimens.HoldAction.innerStroke.toPx()
    val gap = Dimens.HoldAction.ringGap.toPx()
    val outerRadius = (size.minDimension - outerStroke) / 2f
    drawCircle(color = trackColor, radius = outerRadius, style = Stroke(width = outerStroke))
    drawCircle(color = trackColor, radius = outerRadius - gap, style = Stroke(width = innerStroke))
    if (progress > 0f) {
        val topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
        drawArc(
            color = color,
            startAngle = StartAngle,
            sweepAngle = FullSweep * progress,
            useCenter = false,
            topLeft = topLeft,
            size = Size(outerRadius * 2f, outerRadius * 2f),
            style = Stroke(width = Dimens.HoldAction.progressStroke.toPx(), cap = StrokeCap.Round)
        )
    }
}

private const val HoldMillis = 1_200
private const val ReleaseMillis = 250
private const val StartAngle = -90f
private const val FullSweep = 360f

@Composable
fun SparrowLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) color else color.copy(alpha = Alpha.Disabled),
        modifier =
            modifier
                .bleedHorizontally(Dimens.Button.linkPadding)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(
                    enabled = enabled,
                    interactionSource = null,
                    indication = ripple(color = color),
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(horizontal = Dimens.Button.linkPadding, vertical = MaterialTheme.spacing.base)
    )
}

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

@Preview
@Composable
private fun SparrowButtonPreview() {
    SparrowTheme {
        Column {
            SparrowApprovalButton(
                onClick = {},
                text = "Continue"
            )
            SparrowSecondaryButton(
                onClick = {},
                text = "Continue"
            )
            SparrowDestructiveButton(
                onClick = {},
                text = "Delete"
            )
            SparrowOutlinedButton(
                onClick = {},
                text = "Continue"
            )
            SparrowBannerButton(
                onClick = {},
                text = "Continue"
            )
            SparrowRoundApprovalButton(
                onClick = {},
                imageVector = Icons.Filled.Check
            )
        }
    }
}

@Preview
@Composable
private fun SparrowActionPreview() {
    SparrowTheme {
        Column {
            SparrowLink(text = "Why is that?", onClick = {})
        }
    }
}
