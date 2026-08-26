package com.cbgm.sparrow.feature.settings.presentation.developer.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowCardNoAnimation
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_error_log
import com.cbgm.sparrow.resources.feature_settings_error_log_saved_count
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeveloperErrorLogCard(
    savedErrorCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SparrowCardNoAnimation(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.feature_settings_error_log),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.feature_settings_error_log_saved_count, savedErrorCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun DeveloperErrorLogCardPreview() {
    SparrowTheme {
        DeveloperErrorLogCard(
            savedErrorCount = 12,
            onClick = {}
        )
    }
}
