package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.dialog
import com.cbgm.sparrow.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparrowAlertDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: String,
    text: @Composable () -> Unit
) {
    if (isVisible) {
        BasicAlertDialog(onDismissRequest = onDismissRequest) {
            Surface(
                shape = MaterialTheme.shapes.dialog,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
                    Text(
                        text = title,
                        modifier = Modifier.padding(
                            start = Dimens.Dialog.padding,
                            end = Dimens.Dialog.padding,
                            top = Dimens.Dialog.padding,
                            bottom = MaterialTheme.spacing.small
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Dimens.Dialog.padding),
                        thickness = Dimens.Base.dividerThickness,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                    )
                    Box(
                        modifier = Modifier.padding(
                            horizontal = Dimens.Dialog.padding,
                            vertical = MaterialTheme.spacing.medium
                        )
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                            ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                                text()
                            }
                        }
                    }
                    DialogActions(
                        dismissButton = dismissButton,
                        confirmButton = confirmButton
                    )
                }
            }
        }
    }
}

/**
 * Lays the dismiss and confirm slots side by side under a divider, with a divider between them.
 * An empty slot takes no space, so a single slot fills the whole row and no slots draw nothing.
 */
@Composable
private fun DialogActions(
    dismissButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit
) {
    Layout(
        contents = listOf(
            dismissButton,
            confirmButton,
            {
                VerticalDivider(
                    thickness = Dimens.Base.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                )
            },
            {
                HorizontalDivider(
                    thickness = Dimens.Base.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider)
                )
            }
        )
    ) { (dismiss, confirm, between, above), constraints ->
        val slots = listOf(dismiss, confirm).filter { measurables -> measurables.isNotEmpty() }
        if (slots.isEmpty()) {
            return@Layout layout(0, 0) {}
        }
        val width = constraints.maxWidth
        val slotWidth = width / slots.size
        val placeables = slots.map { measurables ->
            measurables.map { measurable ->
                measurable.measure(Constraints(minWidth = slotWidth, maxWidth = slotWidth))
            }
        }
        val slotHeight = placeables.maxOf { row -> row.maxOfOrNull { placeable -> placeable.height } ?: 0 }
        val dividerThickness = Dimens.Base.dividerThickness.roundToPx().coerceAtLeast(1)
        val top = above.map { measurable -> measurable.measure(Constraints.fixed(width, dividerThickness)) }
        val separators = between.map { measurable ->
            measurable.measure(Constraints.fixed(dividerThickness, slotHeight))
        }
        layout(width, dividerThickness + slotHeight) {
            top.forEach { placeable -> placeable.place(0, 0) }
            placeables.forEachIndexed { index, row ->
                row.forEach { placeable -> placeable.place(index * slotWidth, dividerThickness) }
            }
            if (slots.size > 1) {
                separators.forEach { placeable -> placeable.place(slotWidth, dividerThickness) }
            }
        }
    }
}

/**
 * A full-width text action for [SparrowAlertDialog]. Use [color] for the meaning:
 * primary to confirm, error to destroy, onSurfaceVariant to cancel.
 */
@Composable
fun SparrowDialogAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "",
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.Dialog.actionHeight)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = color),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) color else color.copy(alpha = Alpha.Disabled)
        ) {
            ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                if (text.isNotBlank()) {
                    Text(text = text)
                }
                content()
            }
        }
    }
}

@Preview
@Composable
private fun SparrowAlertDialogPreview() {
    SparrowTheme {
        SparrowAlertDialog(
            isVisible = true,
            onDismissRequest = {},
            title = "Reconnect Bob?",
            text = { Text("Sends Bob a new invitation. Your chat history stays.") },
            dismissButton = {
                SparrowDialogAction(
                    onClick = {},
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                SparrowDialogAction(
                    onClick = {},
                    text = "Reconnect",
                    color = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}
