package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.divider
import com.cbgm.sparrow.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparrowDetailRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.outline,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    rippleColor: Color = titleColor
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalRippleConfiguration provides rowRipple(rippleColor)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                enabled = enabled,
                                interactionSource = null,
                                indication = ripple(),
                                onClick = onClick
                            )
                        } else {
                            Modifier
                        }
                    ).padding(vertical = MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.45f)
                )
                Spacer(Modifier.size(MaterialTheme.spacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            titleColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (showChevron) {
                    Icon(
                        imageVector = SparrowIcons.Chevron,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.SettingsScreen.secondaryIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.SettingsScreen.disabledIcon)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = Dimens.SettingsScreen.primaryIconSize + MaterialTheme.spacing.small),
                thickness = Dimens.Base.dividerThickness,
                color = MaterialTheme.colorScheme.divider
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SparrowSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalRippleConfiguration provides rowRipple(MaterialTheme.colorScheme.onSurface)) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        interactionSource = null,
                        indication = ripple(),
                        onClick = { onCheckedChange(!checked) }
                    ).padding(vertical = MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.size(MaterialTheme.spacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                SparrowSwitch(
                    checked = checked,
                    onCheckedChange = null,
                    enabled = enabled
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = Dimens.SettingsScreen.primaryIconSize + MaterialTheme.spacing.small),
                thickness = Dimens.Base.dividerThickness,
                color = MaterialTheme.colorScheme.divider
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun rowRipple(color: Color) = RippleConfiguration(color = color, rippleAlpha = RowRippleAlpha)

private val RowRippleAlpha =
    RippleAlpha(
        pressedAlpha = Alpha.Ripple.pressed,
        focusedAlpha = Alpha.Ripple.pressed,
        draggedAlpha = Alpha.Ripple.pressed,
        hoveredAlpha = Alpha.Ripple.hovered
    )
