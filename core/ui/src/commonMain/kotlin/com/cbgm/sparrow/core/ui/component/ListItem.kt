package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cbgm.sparrow.core.ui.theme.Alpha

@Composable
fun SparrowDialogListItem(
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true
) {
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = isEnabled) { onClick() },
        headlineContent = { Text(text) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.OpaqueText)
        )
    )
}
