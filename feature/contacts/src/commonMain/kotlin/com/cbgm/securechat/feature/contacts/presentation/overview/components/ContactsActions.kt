package com.cbgm.securechat.feature.contacts.presentation.overview.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_create_group
import com.cbgm.securechat.resources.feature_contacts_add_contact_title
import com.cbgm.securechat.resources.feature_contacts_import_from_device
import com.cbgm.securechat.resources.feature_contacts_import_from_device_description
import com.cbgm.securechat.resources.feature_contacts_import_securechat_contact_description
import com.cbgm.securechat.resources.feature_contacts_verify_or_import_identity
import org.jetbrains.compose.resources.stringResource

private val SheetColor = Color(0xFF102A46)

@Composable
fun ContactsFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(50.dp),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.background
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun CreateGroupListItem(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            leadingContent = {
                Box(
                    modifier =
                        Modifier.size(40.dp).background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.feature_chats_create_group),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        containerColor = SheetColor,
        contentColor = Color.White,
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
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(
                        width = 36.dp,
                        height = 4.dp
                    ).background(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(2.dp)
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
                color = Color.White
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
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
                stringResource(Res.string.feature_contacts_import_securechat_contact_description),
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
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
