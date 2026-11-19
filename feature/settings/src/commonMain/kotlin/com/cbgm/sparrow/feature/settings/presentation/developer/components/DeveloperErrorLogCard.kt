package com.cbgm.sparrow.feature.settings.presentation.developer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_error_log
import com.cbgm.sparrow.resources.feature_settings_error_log_saved_count
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeveloperErrorLogCard(
    savedErrorCount: Int,
    onClick: () -> Unit
) {
    SettingsSection(
        title = stringResource(Res.string.feature_settings_error_log)
    ) {
        SparrowDetailRow(
            title = stringResource(Res.string.feature_settings_error_log),
            subtitle = stringResource(
                Res.string.feature_settings_error_log_saved_count,
                savedErrorCount
            ),
            onClick = onClick,
            showDivider = true
        )
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
