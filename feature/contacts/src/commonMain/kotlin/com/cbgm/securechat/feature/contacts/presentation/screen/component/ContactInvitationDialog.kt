package com.cbgm.securechat.feature.contacts.presentation.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_unknown
import com.cbgm.securechat.resources.feature_contacts_accept_invitation
import com.cbgm.securechat.resources.feature_contacts_contact_invitation_description
import com.cbgm.securechat.resources.feature_contacts_contact_invitation_title
import com.cbgm.securechat.resources.feature_contacts_decline_and_block
import com.cbgm.securechat.resources.feature_contacts_decline_invitation
import com.cbgm.securechat.resources.feature_contacts_invitation_phone_number
import com.cbgm.securechat.resources.feature_contacts_invitation_unverified_warning
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactInvitationDialog(
    invitation: PendingContactInvitation,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDeclineAndBlock: () -> Unit
) {
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contacts_contact_invitation_title),
        text = {
            Column {
                Text(
                    text =
                        stringResource(
                            Res.string.feature_contacts_contact_invitation_description,
                            invitation.contactName
                                ?: invitation.contactPhoneNumber
                                ?: stringResource(Res.string.base_unknown)
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
                invitation.contactPhoneNumber?.let { phoneNumber ->
                    Text(
                        text =
                            stringResource(
                                Res.string.feature_contacts_invitation_phone_number,
                                phoneNumber
                            ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = stringResource(Res.string.feature_contacts_invitation_unverified_warning),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.base)
                )
            }
        },
        confirmButton = {
            SecureChatApprovalButton(
                fillMaxWidth = false,
                onClick = onAccept,
                enabled = !isProcessing,
                text = stringResource(Res.string.feature_contacts_accept_invitation)
            )
        },
        dismissButton = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                SecureChatSecondaryButton(
                    fillMaxWidth = false,
                    onClick = onDecline,
                    enabled = !isProcessing,
                    text = stringResource(Res.string.feature_contacts_decline_invitation)
                )
                SecureChatSecondaryButton(
                    fillMaxWidth = false,
                    onClick = onDeclineAndBlock,
                    enabled = !isProcessing,
                    text = stringResource(Res.string.feature_contacts_decline_and_block)
                )
            }
        }
    )
}

@Preview
@Composable
private fun ContactInvitationDialogPreview() {
    SecureChatTheme {
        ContactInvitationDialog(
            invitation =
                PendingContactInvitation(
                    invitationId = "id",
                    contactName = "John Doe",
                    contactPhoneNumber = null,
                    expiresAtEpochMilliseconds = System.currentTimeMillis(),
                    contactId = "2"
                ),
            isProcessing = false,
            onAccept = {},
            onDecline = {},
            onDeclineAndBlock = {}
        )
    }
}
