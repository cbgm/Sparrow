package com.cbgm.sparrow.feature.settings.presentation.network.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowTopBar
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_settings_control_planes
import com.cbgm.sparrow.resources.feature_settings_control_planes_refresh
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPlaneTopBar(
    containerColor: Color,
    isRefreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    SparrowTopBar(
        title = {
            Text(
                text = stringResource(Res.string.feature_settings_control_planes)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = SparrowIcons.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                Icon(
                    imageVector = SparrowIcons.Refresh,
                    contentDescription = stringResource(Res.string.feature_settings_control_planes_refresh)
                )
            }
        },
        containerColor = containerColor
    )
}

@Preview
@Composable
private fun ControlPlaneTopBarPreview() {
    SparrowTheme {
        ControlPlaneTopBar(
            containerColor = MaterialTheme.colorScheme.background,
            isRefreshing = false,
            onBack = {},
            onRefresh = {}
        )
    }
}
