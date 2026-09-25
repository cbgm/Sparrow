package com.cbgm.sparrow.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.spacing

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
        AlertDialog(
            onDismissRequest = onDismissRequest,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small,
            title = {
                Column {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.small))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider))
                }
            },
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton
        )
    }
}
