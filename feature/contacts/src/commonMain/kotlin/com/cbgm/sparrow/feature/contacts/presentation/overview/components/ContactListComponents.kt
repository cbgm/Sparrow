package com.cbgm.sparrow.feature.contacts.presentation.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cbgm.sparrow.core.ui.component.SparrowApprovalButton
import com.cbgm.sparrow.core.ui.component.SparrowAvatar
import com.cbgm.sparrow.core.ui.component.SparrowStatusBadge
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactGroupEntity
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.base_missing
import com.cbgm.sparrow.resources.base_secure
import com.cbgm.sparrow.resources.feature_contacts_could_not_load_contacts
import com.cbgm.sparrow.resources.feature_contacts_no_contacts_hint
import com.cbgm.sparrow.resources.feature_contacts_no_contacts_yet
import com.cbgm.sparrow.resources.feature_contacts_no_phone_number
import com.cbgm.sparrow.resources.feature_contacts_sparrow_contact
import com.cbgm.sparrow.resources.feature_contacts_unnamed_contact
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.contactGroups(
    groups: List<ContactGroupEntity>,
    profilePictures: Map<String, ByteArray?>,
    onContactClick: (Contact) -> Unit,
    trailingContent: @Composable (Contact) -> Unit
) {
    items(
        items = groups,
        key = ContactGroupEntity::title
    ) { group ->
        ContactGroup(
            group = group,
            profilePictures = profilePictures,
            onContactClick = onContactClick,
            trailingContent = trailingContent
        )
    }
}

@Composable
fun ContactStatus(contact: Contact) {
    when {
        contact.deviceContactLinkStatus == DeviceContactLinkStatus.MISSING -> {
            SparrowStatusBadge(
                text = stringResource(Res.string.base_missing),
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error
            )
        }

        contact.sparrowIdentity != null -> {
            SparrowStatusBadge(
                text = stringResource(Res.string.base_secure),
                icon = Icons.Default.Verified,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun ContactSelectionCircle(
    selected: Boolean,
    enabled: Boolean = true
) {
    Box(
        modifier =
            Modifier
                .size(Dimens.ContactsScreen.contactStatusSize)
                .border(
                    width = Dimens.ContactsScreen.contactStatusStroke,
                    color =
                        when {
                            selected -> MaterialTheme.colorScheme.secondary
                            enabled -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        },
                    shape = CircleShape
                ).background(
                    color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                    shape = CircleShape
                ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(Dimens.ContactsScreen.contactStatusIconSize),
                tint = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
fun LoadingContactsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun EmptyContactsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier =
                    Modifier
                        .size(Dimens.ContactsScreen.emptyStateIconContainerSize)
                        .background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(Dimens.ContactsScreen.emptyStateIconSize)
                )
            }

            Text(
                text = stringResource(Res.string.feature_contacts_no_contacts_yet),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(Res.string.feature_contacts_no_contacts_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContactsErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.feature_contacts_could_not_load_contacts),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            actionText?.let { text ->
                SparrowApprovalButton(
                    onClick = onAction,
                    text = text
                )
            }
        }
    }
}

@Composable
private fun ContactGroup(
    group: ContactGroupEntity,
    profilePictures: Map<String, ByteArray?>,
    onContactClick: (Contact) -> Unit,
    trailingContent: @Composable (Contact) -> Unit
) {
    Column {
        Text(
            text = group.title,
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.small
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = Dimens.Card.tonalElevation,
            shadowElevation = Dimens.Card.shadowElevation
        ) {
            Column {
                group.contacts.forEach { contact ->
                    ContactListItem(
                        contact = contact,
                        profilePictureBytes = profilePictures[contact.id],
                        onClick = {
                            onContactClick(contact)
                        },
                        trailingContent = {
                            trailingContent(contact)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    profilePictureBytes: ByteArray?,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            leadingContent = {
                SparrowAvatar(
                    name = contact.displayName ?: "?",
                    pictureBytes = profilePictureBytes
                )
            },
            headlineContent = {
                Text(
                    text =
                        contact.displayName
                            ?: stringResource(Res.string.feature_contacts_unnamed_contact),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text =
                        contact.preferredPhoneNumber?.value
                            ?: if (contact.sparrowIdentity != null) {
                                stringResource(Res.string.feature_contacts_sparrow_contact)
                            } else {
                                stringResource(Res.string.feature_contacts_no_phone_number)
                            },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f)
                )
            },
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
        )
    }
}
