package com.cbgm.sparrow.feature.settings.presentation.overview.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowDialogAction
import com.cbgm.sparrow.core.ui.component.SparrowRadio
import com.cbgm.sparrow.core.ui.locale.AppLanguage
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.base_language
import org.jetbrains.compose.resources.stringResource

@Composable
fun LanguagePickerDialog(
    isVisible: Boolean,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        isVisible = isVisible,
        title = stringResource(Res.string.base_language),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                AppLanguage.entries.forEach { language ->
                    val isSelected = language == currentLanguage

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLanguageSelected(language) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        SparrowRadio(selected = isSelected, onClick = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (language.nativeName != language.displayName) {
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            SparrowDialogAction(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onDismissRequest = onDismiss
    )
}
