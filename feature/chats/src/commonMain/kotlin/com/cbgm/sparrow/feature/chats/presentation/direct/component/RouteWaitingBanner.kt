package com.cbgm.sparrow.feature.chats.presentation.direct.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_route_waiting_description
import com.cbgm.sparrow.resources.feature_chats_route_waiting_title
import org.jetbrains.compose.resources.stringResource

/** Shown while messages wait for a circuit through enough hops, because the user switched off sending directly. */
@Composable
internal fun RouteWaitingBanner(modifier: Modifier = Modifier) {
    val contentColor = MaterialTheme.colorScheme.onErrorContainer
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.micro),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(Dimens.Base.borderStrokeWidth, contentColor.copy(alpha = 0.18f)),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.directConversationScreen.securityBannerVerticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(Dimens.DirectConversationScreen.authorizationIconSize)
            )
            Column(modifier = Modifier.padding(start = MaterialTheme.spacing.small).weight(1f)) {
                Text(
                    text = stringResource(Res.string.feature_chats_route_waiting_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(Res.string.feature_chats_route_waiting_description),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.directConversationScreen.securityDescriptionTopPadding),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Preview
@Composable
private fun RouteWaitingBannerPreview() {
    SparrowTheme {
        RouteWaitingBanner()
    }
}
