package com.cbgm.securechat.feature.contactimport.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatAlertDialog
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatSecondaryButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contactimport.presentation.scan.model.ScannedIdentityPreview
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.feature_contactimport_encryption_key
import com.cbgm.securechat.resources.feature_contactimport_in_person_qr_title
import com.cbgm.securechat.resources.feature_contactimport_qr_trust_warning
import com.cbgm.securechat.resources.feature_contactimport_securechat_identity_found
import com.cbgm.securechat.resources.feature_contactimport_signing_key
import com.cbgm.securechat.resources.feature_contactimport_unnamed_securechat_contact
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScannedIdentityConfirmationDialog(
    preview: ScannedIdentityPreview,
    confirmButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SecureChatAlertDialog(
        onDismissRequest = {},
        title = stringResource(Res.string.feature_contactimport_in_person_qr_title),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                Text(
                    text = preview.displayName ?: stringResource(Res.string.feature_contactimport_unnamed_securechat_contact),
                    style = MaterialTheme.typography.bodyLarge
                )

                preview.phoneNumber?.let { phoneNumber ->
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

                Text(
                    text = stringResource(Res.string.feature_contactimport_securechat_identity_found),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(Res.string.feature_contactimport_qr_trust_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

                FingerprintSection(
                    title = stringResource(Res.string.feature_contactimport_signing_key),
                    fingerprint = preview.signingKeyFingerprint
                )

                FingerprintSection(
                    title = stringResource(Res.string.feature_contactimport_encryption_key),
                    fingerprint = preview.encryptionKeyFingerprint
                )
            }
        },
        confirmButton = {
            SecureChatApprovalButton(
                fillMaxWidth = false,
                onClick = onConfirm,
                text = confirmButtonText
            )
        },
        dismissButton = {
            SecureChatSecondaryButton(
                fillMaxWidth = false,
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel)
            )
        }
    )
}

@Composable
private fun FingerprintSection(
    title: String,
    fingerprint: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = fingerprint,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview
@Composable
fun ScannedIdentityDialogPreview() {
    SecureChatTheme {
        ScannedIdentityConfirmationDialog(
            preview =
                ScannedIdentityPreview(
                    displayName = "John Doe",
                    phoneNumber = "1234567890",
                    signingKeyFingerprint = "12:34:5",
                    encryptionKeyFingerprint = "12:34:5",
                    encodedIdentity = "6465sd4f5s4f6sf4s"
                ),
            confirmButtonText = "Confirm",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
