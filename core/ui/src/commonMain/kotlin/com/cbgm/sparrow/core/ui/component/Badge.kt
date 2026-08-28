package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.badge
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun SparrowStatusBadge(
    text: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = MaterialTheme.shapes.badge,
        color = color.copy(alpha = Alpha.Badge.container)
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                    vertical = MaterialTheme.spacing.micro
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Dimens.Badge.iconSize)
            )

            Text(
                text = text,
                modifier = Modifier.padding(start = MaterialTheme.spacing.micro),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
