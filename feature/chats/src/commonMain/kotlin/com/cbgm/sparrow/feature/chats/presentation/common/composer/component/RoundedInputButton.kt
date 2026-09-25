package com.cbgm.sparrow.feature.chats.presentation.common.composer.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.cbgm.sparrow.core.ui.theme.Dimens

@Composable
internal fun RoundedInputButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: ImageVector,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier.requiredSize(Dimens.MessageInput.buttonHeight)
    ) {
        Box(
            modifier = Modifier
                .requiredSize(Dimens.MessageInput.buttonHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, CircleShape)
                .border(
                    width = Dimens.Base.borderStrokeWidth,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(Dimens.MessageInput.iconSize)
            )
        }
    }
}
