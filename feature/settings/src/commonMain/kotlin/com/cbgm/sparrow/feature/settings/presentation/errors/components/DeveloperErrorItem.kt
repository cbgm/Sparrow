package com.cbgm.sparrow.feature.settings.presentation.errors.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.settings.presentation.errors.model.DeveloperErrorUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_error_details_hide
import com.cbgm.sparrow.resources.feature_settings_error_details_show
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeveloperErrorItem(
    error: DeveloperErrorUi,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(error.id) { mutableStateOf(false) }

    SparrowCardNoAnimation(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error.timestamp,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = error.tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

            error.exceptionType?.let { exceptionType ->
                Text(
                    text = exceptionType,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!error.stackTrace.isNullOrBlank()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(top = MaterialTheme.spacing.base),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text =
                            stringResource(
                                if (expanded) {
                                    Res.string.feature_settings_error_details_hide
                                } else {
                                    Res.string.feature_settings_error_details_show
                                }
                            ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (expanded) {
                    Text(
                        text = error.stackTrace,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DeveloperErrorItemPreview() {
    SparrowTheme {
        DeveloperErrorItem(
            error =
                DeveloperErrorUi(
                    id = "preview",
                    timestamp = "25.08.2026 03:35:42.123",
                    tag = "DefaultOutboxRunner",
                    message = "Outgoing message failed",
                    exceptionType = "IllegalStateException",
                    stackTrace = "java.lang.IllegalStateException: Invalid outbox transition\n    at DefaultOutboxRunner.send(...)"
                )
        )
    }
}
