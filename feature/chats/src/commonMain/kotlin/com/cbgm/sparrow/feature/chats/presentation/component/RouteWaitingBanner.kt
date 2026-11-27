package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowBanner
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.core.ui.theme.warning
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_route_waiting_description
import com.cbgm.sparrow.resources.feature_chats_route_waiting_title
import org.jetbrains.compose.resources.stringResource

/** Shown while messages wait for a circuit through enough hops, because the user switched off sending directly. */
@Composable
internal fun RouteWaitingBanner(modifier: Modifier = Modifier) {
    SparrowBanner(
        icon = Icons.Default.Schedule,
        accent = MaterialTheme.colorScheme.warning,
        title = stringResource(Res.string.feature_chats_route_waiting_title),
        description = stringResource(Res.string.feature_chats_route_waiting_description),
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.micro)
    )
}

@Preview
@Composable
private fun RouteWaitingBannerPreview() {
    SparrowTheme {
        RouteWaitingBanner()
    }
}
