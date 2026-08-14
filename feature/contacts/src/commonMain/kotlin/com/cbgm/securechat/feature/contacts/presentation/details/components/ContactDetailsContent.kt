package com.cbgm.securechat.feature.contacts.presentation.details.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_share_contact
import com.cbgm.securechat.resources.feature_contacts_share_contact_missing_keys
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactDetailsContent(
    contact: Contact,
    safetyNumber: SafetyNumber?,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = MaterialTheme.spacing.screenPadding,
                    end = MaterialTheme.spacing.screenPadding
                ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        ContactHeader(contact = contact)

        SecureChatApprovalButton(
            onClick = onShareContact,
            enabled = contact.secureChatIdentity != null,
            content = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                Text(
                    text = stringResource(Res.string.base_share_contact),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )

        if (contact.secureChatIdentity == null) {
            Text(
                text = stringResource(Res.string.feature_contacts_share_contact_missing_keys),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ContactDetailsSectionCard {
            ContactPhoneNumbersSection(
                phoneNumbers = contact.phoneNumbers,
                preferredPhoneNumberId = contact.preferredPhoneNumberId
            )
        }

        ContactDetailsSectionCard {
            DeviceContactSection(status = contact.deviceContactLinkStatus)
        }

        ContactDetailsSectionCard {
            val identity = contact.secureChatIdentity
            if (identity == null) {
                NoSecureChatIdentityContent()
            } else {
                SecureChatIdentitySection(
                    identity = identity,
                    safetyNumber = safetyNumber,
                    onVerifyIdentity = onVerifyIdentity
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    }
}

@Preview
@Composable
private fun ContactDetailsContentPreview() {
    SecureChatTheme {
        ContactDetailsContent(
            contact = ContactDetailsPreviewData.contact,
            safetyNumber = ContactDetailsPreviewData.safetyNumber,
            onShareContact = {},
            onVerifyIdentity = {},
            scrollState = rememberScrollState(),
            innerPadding = PaddingValues(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
