package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.spacing

@Composable
fun SparrowDialogListItem(
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.micro)
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text(text, style = MaterialTheme.typography.bodyMedium) },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (isEnabled) Alpha.OpaqueText else Alpha.Disabled
                )
            )
        )
    }
}
