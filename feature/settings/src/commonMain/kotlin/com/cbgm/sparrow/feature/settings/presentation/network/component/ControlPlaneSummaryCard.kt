package com.cbgm.sparrow.feature.settings.presentation.network.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_planes_auto_health_brief
import com.cbgm.sparrow.resources.feature_settings_control_planes_summary
import org.jetbrains.compose.resources.stringResource

@Composable
fun ControlPlaneSummaryCard(
    availableCount: Int,
    unavailableCount: Int,
    modifier: Modifier = Modifier
) {
    ControlPlaneSettingsGroup(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.SettingsScreen.primaryIconSize)
            )
            Spacer(Modifier.size(MaterialTheme.spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        Res.string.feature_settings_control_planes_summary,
                        availableCount,
                        unavailableCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.feature_settings_control_planes_auto_health_brief),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun ControlPlaneSummaryCardPreview() {
    SparrowTheme {
        ControlPlaneSummaryCard(
            availableCount = 3,
            unavailableCount = 1
        )
    }
}
