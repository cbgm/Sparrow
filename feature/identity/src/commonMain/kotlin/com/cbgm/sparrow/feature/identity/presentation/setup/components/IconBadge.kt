package com.cbgm.sparrow.feature.identity.presentation.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.secondary
) {
    Box(
        modifier =
            Modifier
                .size(Dimens.IdentityScreen.iconBadgeSize)
                .background(tint.copy(alpha = 0.12f), CircleShape)
                .border(Dimens.IdentityScreen.iconBadgeBorderWidth, tint.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(Dimens.IdentityScreen.iconBadgeIconSize)
        )
    }
}

@Preview
@Composable
fun IconBadgePreview() {
    SparrowTheme {
        IconBadge(
            icon = androidx.compose.material.icons.Icons.Default.Lock,
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}
