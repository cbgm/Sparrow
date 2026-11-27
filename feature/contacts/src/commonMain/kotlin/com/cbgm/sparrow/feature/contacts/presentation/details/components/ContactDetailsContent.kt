package com.cbgm.sparrow.feature.contacts.presentation.details.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.component.SparrowDetailRow
import com.cbgm.sparrow.core.ui.icon.SparrowIcons
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.errorBright
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsContactUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.DeviceContactLinkUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_linked
import com.cbgm.sparrow.resources.base_phone_numbers
import com.cbgm.sparrow.resources.base_share_contact
import com.cbgm.sparrow.resources.feature_attachments_media_and_files
import com.cbgm.sparrow.resources.feature_chats_reconnect
import com.cbgm.sparrow.resources.feature_contacts_device_contact
import com.cbgm.sparrow.resources.feature_contacts_device_contact_missing
import com.cbgm.sparrow.resources.feature_contacts_no_sparrow_identity
import com.cbgm.sparrow.resources.feature_contacts_not_linked
import com.cbgm.sparrow.resources.feature_contacts_share_contact_missing_keys
import com.cbgm.sparrow.resources.feature_contacts_sparrow_contact_not_verified
import com.cbgm.sparrow.resources.feature_contacts_sparrow_identity
import com.cbgm.sparrow.resources.feature_contacts_verified_by_contact
import com.cbgm.sparrow.resources.feature_contacts_verified_by_you
import com.cbgm.sparrow.resources.feature_contacts_verified_sparrow_contact
import org.jetbrains.compose.resources.stringResource

internal enum class ContactDetailPage { Overview, Phones, Identity }

@Composable
internal fun ContactDetailsContent(
    contact: ContactDetailsContactUi,
    safetyNumber: String?,
    modifier: Modifier = Modifier,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit,
    onMediaAndFiles: () -> Unit,
    onReconnect: () -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    page: ContactDetailPage = ContactDetailPage.Overview,
    onPageSelected: (ContactDetailPage) -> Unit = {}
) {
    Column(
        modifier = modifier.verticalScroll(scrollState).padding(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding(),
            start = MaterialTheme.spacing.screenPadding,
            end = MaterialTheme.spacing.screenPadding
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        when (page) {
            ContactDetailPage.Overview -> {
                val identity = contact.sparrowIdentity
                ContactHeader(contact = contact)

                Column {
                    SparrowDetailRow(
                        icon = SparrowIcons.Phone,
                        title = stringResource(Res.string.base_phone_numbers),
                        subtitle = contact.preferredPhoneNumber,
                        onClick = { onPageSelected(ContactDetailPage.Phones) }
                    )
                    SparrowDetailRow(
                        icon = SparrowIcons.ContactCard,
                        title = stringResource(Res.string.feature_contacts_device_contact),
                        showChevron = false,
                        subtitle = when (contact.deviceContactLinkStatus) {
                            DeviceContactLinkUi.LINKED -> stringResource(Res.string.base_linked)
                            DeviceContactLinkUi.NOT_LINKED -> stringResource(Res.string.feature_contacts_not_linked)
                            DeviceContactLinkUi.MISSING -> stringResource(Res.string.feature_contacts_device_contact_missing)
                        },
                        onClick = null
                    )
                    SparrowDetailRow(
                        icon = SparrowIcons.Shield,
                        title = stringResource(Res.string.feature_contacts_sparrow_identity),
                        subtitle = when {
                            identity == null -> stringResource(Res.string.feature_contacts_no_sparrow_identity)
                            identity.verifiedByMe && identity.mutualKeyExchange && identity.verifiedByContact ->
                                stringResource(Res.string.feature_contacts_verified_sparrow_contact)
                            identity.verifiedByMe -> stringResource(Res.string.feature_contacts_verified_by_you)
                            identity.mutualKeyExchange && identity.verifiedByContact ->
                                stringResource(Res.string.feature_contacts_verified_by_contact)
                            else -> stringResource(Res.string.feature_contacts_sparrow_contact_not_verified)
                        },
                        onClick = { onPageSelected(ContactDetailPage.Identity) }
                    )
                    SparrowDetailRow(
                        icon = SparrowIcons.Folder,
                        title = stringResource(Res.string.feature_attachments_media_and_files),
                        onClick = onMediaAndFiles
                    )
                    SparrowDetailRow(
                        icon = SparrowIcons.Share,
                        title = stringResource(Res.string.base_share_contact),
                        enabled = identity != null,
                        showChevron = false,
                        onClick = onShareContact
                    )
                    SparrowDetailRow(
                        icon = SparrowIcons.Refresh,
                        title = stringResource(Res.string.feature_chats_reconnect),
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        rippleColor = MaterialTheme.colorScheme.errorBright,
                        showChevron = false,
                        showDivider = false,
                        onClick = onReconnect
                    )
                }
                if (identity == null) {
                    Text(
                        text = stringResource(Res.string.feature_contacts_share_contact_missing_keys),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)
                    )
                }
            }
            ContactDetailPage.Phones -> {
                Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)) {
                    ContactPhoneNumbersSection(
                        phoneNumbers = contact.phoneNumbers,
                        preferredPhoneNumberId = contact.preferredPhoneNumberId,
                        showTitle = false
                    )
                }
            }
            ContactDetailPage.Identity -> {
                Column(modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)) {
                    val identity = contact.sparrowIdentity
                    if (identity == null) {
                        NoSparrowIdentityContent()
                    } else {
                        SparrowIdentitySection(
                            identity = identity,
                            safetyNumber = safetyNumber,
                            onVerifyIdentity = onVerifyIdentity,
                            showTitle = false
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
    }
}

@Preview
@Composable
private fun ContactDetailsContentPreview() {
    SparrowTheme {
        ContactDetailsContent(
            contact = ContactDetailsPreviewData.contact,
            safetyNumber = ContactDetailsPreviewData.safetyNumber,
            onShareContact = {},
            onVerifyIdentity = {},
            onMediaAndFiles = {},
            onReconnect = {},
            scrollState = rememberScrollState(),
            innerPadding = PaddingValues(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
