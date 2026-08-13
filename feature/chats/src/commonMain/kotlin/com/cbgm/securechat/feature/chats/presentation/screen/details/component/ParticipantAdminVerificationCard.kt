package com.cbgm.securechat.feature.chats.presentation.screen.details.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.GroupDetailsPreviewData
import com.cbgm.securechat.feature.chats.presentation.component.groupdetails.verificationStatusText
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_verify_contact
import com.cbgm.securechat.resources.feature_chats_group_admin
import com.cbgm.securechat.resources.feature_chats_group_verify_admin_description
import com.cbgm.securechat.resources.feature_chats_group_verify_admin_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ParticipantAdminVerificationCard(
    admin: GroupMemberVerificationUiState,
    onVerify: () -> Unit
) {
    val adminName =
        admin.displayName.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.feature_chats_group_admin)

    SecureChatCardNoAnimation(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Text(
                text = stringResource(Res.string.feature_chats_group_verify_admin_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_verify_admin_description),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = admin.verificationStatusText(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.small)
            )

            if (admin.canVerify && admin.contactId != null) {
                SecureChatApprovalButton(
                    onClick = onVerify,
                    text = stringResource(Res.string.base_verify_contact, adminName)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ParticipantAdminVerificationCardPreview() {
    SecureChatTheme {
        ParticipantAdminVerificationCard(
            admin = GroupDetailsPreviewData.admin.copy(canVerify = true),
            onVerify = {}
        )
    }
}
