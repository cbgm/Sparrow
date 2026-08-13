package com.cbgm.securechat.feature.contacts.presentation.details.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_linked
import com.cbgm.securechat.resources.feature_contacts_device_contact
import com.cbgm.securechat.resources.feature_contacts_device_contact_linked_description
import com.cbgm.securechat.resources.feature_contacts_device_contact_missing
import com.cbgm.securechat.resources.feature_contacts_device_contact_missing_description
import com.cbgm.securechat.resources.feature_contacts_device_contact_not_linked_description
import com.cbgm.securechat.resources.feature_contacts_not_linked
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceContactSection(status: DeviceContactLinkStatus) {
    SectionTitle(
        icon = Icons.Default.ContactPhone,
        title = stringResource(Res.string.feature_contacts_device_contact)
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    when (status) {
        DeviceContactLinkStatus.NOT_LINKED ->
            ContactStatusRow(
                icon = Icons.Default.LinkOff,
                iconColor = MaterialTheme.colorScheme.error,
                title = stringResource(Res.string.feature_contacts_not_linked),
                titleColor = MaterialTheme.colorScheme.error,
                description = stringResource(Res.string.feature_contacts_device_contact_not_linked_description)
            )

        DeviceContactLinkStatus.LINKED ->
            ContactStatusRow(
                icon = Icons.Default.Link,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = stringResource(Res.string.base_linked),
                titleColor = MaterialTheme.colorScheme.secondary,
                description = stringResource(Res.string.feature_contacts_device_contact_linked_description)
            )

        DeviceContactLinkStatus.MISSING ->
            ContactStatusRow(
                icon = Icons.Default.LinkOff,
                iconColor = MaterialTheme.colorScheme.error.copy(alpha = 0.74f),
                title = stringResource(Res.string.feature_contacts_device_contact_missing),
                titleColor = MaterialTheme.colorScheme.error.copy(alpha = 0.74f),
                description = stringResource(Res.string.feature_contacts_device_contact_missing_description)
            )
    }
}

@Preview
@Composable
private fun DeviceContactSectionPreview() {
    SecureChatTheme {
        DeviceContactSection(status = DeviceContactLinkStatus.LINKED)
    }
}
