package com.cbgm.securechat.feature.contacts.presentation.component.contactdetails

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.presentation.screen.details.component.SectionTitle
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_not_verified
import com.cbgm.securechat.resources.feature_contacts_compare_before_trusting
import com.cbgm.securechat.resources.feature_contacts_encryption_fingerprint
import com.cbgm.securechat.resources.feature_contacts_identity_verified_description
import com.cbgm.securechat.resources.feature_contacts_mutually_verified
import com.cbgm.securechat.resources.feature_contacts_securechat_identity
import com.cbgm.securechat.resources.feature_contacts_signing_fingerprint
import com.cbgm.securechat.resources.feature_contacts_verified_by_contact
import com.cbgm.securechat.resources.feature_contacts_verified_by_contact_description
import com.cbgm.securechat.resources.feature_contacts_verified_by_you
import com.cbgm.securechat.resources.feature_contacts_verified_by_you_description
import com.cbgm.securechat.resources.feature_contacts_verify_safety_number
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SecureChatIdentitySection(
    identity: SecureChatIdentity,
    safetyNumber: SafetyNumber?,
    onVerifyIdentity: () -> Unit
) {
    SectionTitle(
        icon = Icons.Default.Security,
        title = stringResource(Res.string.feature_contacts_securechat_identity)
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    val verifiedByContact =
        identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL && identity.verifiedByContact

    when {
        identity.verificationStatus == ContactVerificationStatus.VERIFIED && verifiedByContact ->
            ContactStatusRow(
                icon = Icons.Default.Link,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = stringResource(Res.string.feature_contacts_mutually_verified),
                titleColor = MaterialTheme.colorScheme.secondary,
                description = stringResource(Res.string.feature_contacts_identity_verified_description)
            )

        identity.verificationStatus == ContactVerificationStatus.VERIFIED ->
            ContactStatusRow(
                icon = Icons.Default.Schedule,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = stringResource(Res.string.feature_contacts_verified_by_you),
                titleColor = MaterialTheme.colorScheme.secondary,
                description = stringResource(Res.string.feature_contacts_verified_by_you_description)
            )

        verifiedByContact ->
            ContactStatusRow(
                icon = Icons.Default.Security,
                iconColor = MaterialTheme.colorScheme.tertiary,
                title = stringResource(Res.string.feature_contacts_verified_by_contact),
                titleColor = MaterialTheme.colorScheme.tertiary,
                description = stringResource(Res.string.feature_contacts_verified_by_contact_description)
            )

        else ->
            ContactStatusRow(
                icon = Icons.Default.LinkOff,
                iconColor = MaterialTheme.colorScheme.error,
                title = stringResource(Res.string.base_not_verified),
                titleColor = MaterialTheme.colorScheme.error,
                description = stringResource(Res.string.feature_contacts_compare_before_trusting)
            )
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    if (safetyNumber != null) {
        if (identity.verificationStatus == ContactVerificationStatus.UNVERIFIED) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            SecureChatApprovalButton(
                onClick = onVerifyIdentity,
                content = {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                    Text(text = stringResource(Res.string.feature_contacts_verify_safety_number))
                }
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }

    IdentityKeySection(
        title = stringResource(Res.string.feature_contacts_signing_fingerprint),
        key = identity.signingPublicKey
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    IdentityKeySection(
        title = stringResource(Res.string.feature_contacts_encryption_fingerprint),
        key = identity.encryptionPublicKey
    )
}

@Preview
@Composable
private fun SecureChatIdentitySectionPreview() {
    SecureChatTheme {
        SecureChatIdentitySection(
            identity = ContactDetailsPreviewData.identity,
            safetyNumber = ContactDetailsPreviewData.safetyNumber,
            onVerifyIdentity = {}
        )
    }
}
