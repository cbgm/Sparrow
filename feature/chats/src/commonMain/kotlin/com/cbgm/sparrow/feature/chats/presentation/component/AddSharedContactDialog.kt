package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowAlertDialog
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowSecondaryButton
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_cancel
import com.cbgm.sparrow.resources.feature_chats_add_shared_contact_description
import com.cbgm.sparrow.resources.feature_contacts_add_contact
import com.cbgm.sparrow.resources.feature_contacts_add_contact_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AddSharedContactDialog(
    contact: SharedContact,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SparrowAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.feature_contacts_add_contact_title),
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)
            ) {
                Text(
                    text = stringResource(Res.string.feature_chats_add_shared_contact_description),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column(
                    modifier = Modifier
                        .padding(vertical = MaterialTheme.spacing.base)
                        .padding(MaterialTheme.spacing.base)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    contact.displayName?.takeIf(String::isNotBlank)?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            SparrowApprovalButton(
                onClick = onConfirm,
                text = stringResource(Res.string.feature_contacts_add_contact),
                fillMaxWidth = false
            )
        },
        dismissButton = {
            SparrowSecondaryButton(
                onClick = onDismiss,
                text = stringResource(Res.string.base_cancel),
                fillMaxWidth = false
            )
        }
    )
}

@Preview
@Composable
private fun AddSharedContactDialogPreview() {
    SparrowTheme {
        AddSharedContactDialog(
            contact = SharedContact(displayName = "Alex", phoneNumber = "+49 151 12345678"),
            onConfirm = {},
            onDismiss = {}
        )
    }
}
