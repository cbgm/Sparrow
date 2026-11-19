package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

@Composable
fun SparrowSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = Color.Transparent,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Disabled),
                disabledCheckedTrackColor = Color.Transparent,
                disabledCheckedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Disabled),
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline.copy(alpha = Alpha.Disabled),
                disabledUncheckedTrackColor = Color.Transparent,
                disabledUncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = Alpha.Disabled)
            )
    )
}

@Composable
fun SparrowRadio(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors =
            RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline
            )
    )
}

@Composable
fun SparrowSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(Dimens.Slider.trackHeight),
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null
            )
        },
        thumb = {
            Box(
                modifier = Modifier
                    .height(Dimens.Slider.thumbHeight)
                    .width(Dimens.Slider.thumbWidth)
                    .background(MaterialTheme.colorScheme.primary, RectangleShape)
            )
        }
    )
}

@Composable
fun SparrowFloatingButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(Dimens.FloatingButton.size),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation()
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
fun SparrowChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon =
            icon?.let { image ->
                { Icon(imageVector = image, contentDescription = null, modifier = Modifier.size(Dimens.Chip.iconSize)) }
            },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconColor = MaterialTheme.colorScheme.outline,
                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.Chip.selected),
                selectedLabelColor = MaterialTheme.colorScheme.primary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = enabled,
                selected = selected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = Color.Transparent,
                borderWidth = Dimens.Base.borderStrokeWidth
            )
    )
}

@Preview
@Composable
private fun SparrowControlsPreview() {
    SparrowTheme {
        Column {
            SparrowSwitch(checked = true, onCheckedChange = {})
            SparrowSwitch(checked = false, onCheckedChange = {})
            SparrowRadio(selected = true, onClick = {})
            SparrowChip(selected = true, onClick = {}, label = "Food")
            SparrowFloatingButton(icon = SparrowIcons.Add, onClick = {})
            SparrowSlider(
                value = 0.5f,
                onValueChange = {},
                onValueChangeFinished = {},
                valueRange = 0f..1f
            )
        }
    }
}
