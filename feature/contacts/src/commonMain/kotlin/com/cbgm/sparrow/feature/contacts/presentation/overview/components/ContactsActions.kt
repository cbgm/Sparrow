package com.cbgm.sparrow.feature.contacts.presentation.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.cbgm.sparrow.core.ui.theme.Alpha
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.circle
import com.cbgm.sparrow.core.ui.theme.contactsScreen
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_create_group
import com.cbgm.sparrow.resources.feature_contacts_add_contact_title
import com.cbgm.sparrow.resources.feature_contacts_import_from_device
import com.cbgm.sparrow.resources.feature_contacts_import_from_device_description
import com.cbgm.sparrow.resources.feature_contacts_import_sparrow_contact_description
import com.cbgm.sparrow.resources.feature_contacts_verify_or_import_identity
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContactsFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(Dimens.ContactsScreen.addContactButtonSize),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(Dimens.ContactsScreen.addContactIconSize)
        )
    }
}

@Composable
fun CreateGroupListItem(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = Dimens.Card.tonalElevation,
        shadowElevation = Dimens.Card.shadowElevation
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            leadingContent = {
                Box(
                    modifier =
                        Modifier.size(Dimens.ContactsScreen.menuIconContainerSize).background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = Alpha.ContactsScreen.actionIconBackground),
                            shape = MaterialTheme.shapes.circle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.feature_chats_create_group),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactBottomSheet(
    onDismiss: () -> Unit,
    onImportContact: () -> Unit,
    onImportDeviceContacts: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null
    ) {
        ImportContactSheet(
            onClose = onDismiss,
            onImportContact = onImportContact,
            onImportDeviceContacts = onImportDeviceContacts
        )
    }
}

@Composable
private fun ImportContactSheet(
    onClose: () -> Unit,
    onImportContact: () -> Unit,
    onImportDeviceContacts: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.medium)
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = MaterialTheme.spacing.contactsScreen.dragHandleTopPadding)
                    .align(Alignment.CenterHorizontally)
                    .size(
                        width = Dimens.ContactsScreen.dragHandleWidth,
                        height = Dimens.ContactsScreen.dragHandleHeight
                    ).background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.ContactsScreen.scanOverlay),
                        shape = MaterialTheme.shapes.contactsScreen.dragHandle
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.feature_contacts_add_contact_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        ImportOptionRow(
            icon = Icons.Default.Contacts,
            title = stringResource(Res.string.feature_contacts_import_from_device),
            description = stringResource(Res.string.feature_contacts_import_from_device_description),
            onClick = onImportDeviceContacts
        )

        ImportOptionRow(
            icon = Icons.Default.PersonAdd,
            title = stringResource(Res.string.feature_contacts_verify_or_import_identity),
            description =
                stringResource(Res.string.feature_contacts_import_sparrow_contact_description),
            onClick = onImportContact
        )
    }
}

@Composable
private fun ImportOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Alpha.ContactsScreen.scanIcon),
                modifier = Modifier.size(Dimens.ContactsScreen.menuItemIconSize)
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = Alpha.OpaqueText)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
