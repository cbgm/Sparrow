package com.cbgm.securechat.feature.chats.presentation.screen.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatBannerButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_accept
import com.cbgm.securechat.resources.feature_chats_group_decline
import com.cbgm.securechat.resources.feature_chats_group_invitation_description
import com.cbgm.securechat.resources.feature_chats_group_invitation_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun GroupInvitationHint(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.screenPadding,
                        vertical = MaterialTheme.spacing.small
                    )
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_invitation_title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_invitation_description),
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.spacing.base),
                horizontalArrangement = Arrangement.End
            ) {
                SecureChatBannerButton(
                    onClick = onDecline,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.feature_chats_group_decline)
                )

                SecureChatBannerButton(
                    onClick = onAccept,
                    fillMaxWidth = false,
                    text = stringResource(Res.string.feature_chats_group_accept)
                )
            }
        }
    }
}

@Preview
@Composable
fun GroupInvitationBannerPreview() {
    SecureChatTheme {
        GroupInvitationHint(
            onAccept = {},
            onDecline = {}
        )
    }
}
