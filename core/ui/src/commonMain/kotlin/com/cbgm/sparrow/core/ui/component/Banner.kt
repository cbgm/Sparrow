package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.asTint
import com.cbgm.sparrow.core.ui.theme.spacing

data class SparrowBannerAction(
    val text: String,
    val onClick: () -> Unit
)

@Composable
fun SparrowBanner(
    icon: ImageVector,
    accent: Color,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actions: List<SparrowBannerAction> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Dimens.Banner.iconSize)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let { text ->
                    Text(
                        text = text,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.micro.div(2)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (actions.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                        actions.forEach { action ->
                            SparrowLink(text = action.text, onClick = action.onClick, color = accent)
                        }
                    }
                }
            }
        }
    }
    val bannerModifier = modifier.fillMaxWidth()
    val color = accent.asTint(Alpha.Banner.tint)
    if (onClick == null) {
        Surface(
            modifier = bannerModifier,
            shape = MaterialTheme.shapes.medium,
            color = color,
            content = content
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = bannerModifier,
            shape = MaterialTheme.shapes.medium,
            color = color,
            content = content
        )
    }
}

@Preview
@Composable
private fun SparrowBannerPreview() {
    SparrowTheme {
        SparrowBanner(
            icon = Icons.Default.Warning,
            accent = MaterialTheme.colorScheme.error,
            title = "Not verified yet",
            description = "Compare the safety number with Bob.",
            actions = listOf(SparrowBannerAction(text = "Verify", onClick = {}))
        )
    }
}
