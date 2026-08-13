package com.cbgm.securechat.feature.contacts.presentation.screen.details.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_contacts_no_securechat_identity
import com.cbgm.securechat.resources.feature_contacts_securechat_contact_not_verified
import com.cbgm.securechat.resources.feature_contacts_unnamed_contact
import com.cbgm.securechat.resources.feature_contacts_verified_by_contact
import com.cbgm.securechat.resources.feature_contacts_verified_by_you
import com.cbgm.securechat.resources.feature_contacts_verified_securechat_contact
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactHeader(contact: Contact) {
    val identity = contact.secureChatIdentity
    val verifiedByMe = identity?.verificationStatus == ContactVerificationStatus.VERIFIED
    val verifiedByContact =
        identity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL && identity.verifiedByContact
    val isMutuallyVerified = verifiedByMe && verifiedByContact

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contact.initials(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                isMutuallyVerified ->
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF071A2E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                verifiedByMe ->
                    ContactVerificationBadge(
                        icon = Icons.Default.Schedule,
                        containerColor = MaterialTheme.colorScheme.secondary
                    )

                verifiedByContact ->
                    ContactVerificationBadge(
                        icon = Icons.Default.Security,
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )

                identity != null ->
                    ContactVerificationBadge(
                        icon = Icons.Default.Warning,
                        containerColor = MaterialTheme.colorScheme.error
                    )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = contact.displayName ?: stringResource(Res.string.feature_contacts_unnamed_contact),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text =
                when {
                    identity == null ->
                        stringResource(Res.string.feature_contacts_no_securechat_identity)

                    isMutuallyVerified ->
                        stringResource(Res.string.feature_contacts_verified_securechat_contact)

                    verifiedByMe ->
                        stringResource(Res.string.feature_contacts_verified_by_you)

                    verifiedByContact ->
                        stringResource(Res.string.feature_contacts_verified_by_contact)

                    else ->
                        stringResource(Res.string.feature_contacts_securechat_contact_not_verified)
                },
            style = MaterialTheme.typography.bodyMedium,
            color =
                when {
                    identity == null -> MaterialTheme.colorScheme.error
                    isMutuallyVerified || verifiedByMe -> MaterialTheme.colorScheme.secondary
                    verifiedByContact -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                },
            textAlign = TextAlign.Center
        )
    }
}

private fun Contact.initials(): String =
    displayName
        ?.trim()
        ?.split(regex = Regex("\\s+"))
        ?.filter(String::isNotBlank)
        ?.take(2)
        ?.mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
        ?.joinToString(separator = "")
        ?.takeIf(String::isNotBlank)
        ?: "?"

@Preview
@Composable
private fun ContactHeaderPreview() {
    SecureChatTheme {
        ContactHeader(contact = ContactDetailsPreviewData.contact)
    }
}
